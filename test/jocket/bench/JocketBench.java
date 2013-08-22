package jocket.bench;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import jocket.net.JocketSocket;
import jocket.net.ServerJocket;

public class JocketBench {

	private final int niter;
	private final int datasize;
	private DataOutputStream out;
	private DataInputStream in;
	private byte[] buf;
	private final long[] res;
	private String fileName;

	public JocketBench(int niter, int datasize, boolean useJocket)
	    throws IOException {
		this.niter = niter;
		this.datasize = datasize;
		res = new long[niter];
		if (useJocket)
			initJ();
		else
			initNet();
	}

	private void initJ() throws IOException {
		fileName = "Jocket";
		final ServerJocket srv = new ServerJocket(0);
		new Thread() {
			@Override
			public void run() {
				try {
					JocketSocket s = srv.accept();
					srv.close();
					answerLoop(s.getInputStream(), s.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		JocketSocket s = new JocketSocket(srv.getLocalPort());
		in = new DataInputStream(s.getInputStream());
		out = new DataOutputStream(s.getOutputStream());
		buf = new byte[datasize];
	}

	private void initNet() throws IOException {
		fileName = "Socket";
		final ServerSocket srv = new ServerSocket(0);
		new Thread() {
			@Override
			public void run() {
				try {
					Socket s = srv.accept();
					srv.close();
					s.setTcpNoDelay(true);
					answerLoop(s.getInputStream(),
					    new BufferedOutputStream(s.getOutputStream()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		Socket s = new Socket("localhost", srv.getLocalPort());
		s.setTcpNoDelay(true);
		in = new DataInputStream(s.getInputStream());
		out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
		buf = new byte[datasize];
	}

	protected void answerLoop(InputStream in, OutputStream out)
	    throws IOException {
		DataInputStream din = new DataInputStream(in);
		byte[] buf = new byte[1024 * 1024];
		for (int i = 0; i < niter; i++) {
			int size = din.readInt();
			// din.readFully(buf, 0, size);
			out.write(buf, 0, size);
			out.flush();
		}
	}

	public void bench() throws IOException {
		for (int i = 0; i < niter; i++) {
			iter(i);
		}
		dumpResults(new FileOutputStream("/tmp/" + fileName));
	}

	public void iter(int i) throws IOException {
		// if (i % 1000 == 0)
		// System.out.println(i);
		long start = System.nanoTime();
		out.writeInt(datasize);
		// out.write(buf, 0, datasize);
		out.flush();
		in.readFully(buf, 0, datasize);
		long stop = System.nanoTime();
		res[i] = stop - start;
	}

	private void dumpResults(OutputStream out) throws IOException {
		PrintWriter w = new PrintWriter(out);
		for (int i = 0; i < res.length; i++) {
			w.println(res[i] / 1000.0);
		}
		w.flush();
		w.close();
	}

	public static void main(String[] args) throws IOException {
		new JocketBench(50000, 200, true).bench();
		new JocketBench(50000, 200, false).bench();
	}

}
