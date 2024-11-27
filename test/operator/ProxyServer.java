package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ProxyServer {

    public static void main(String[] args) {
        try {
            String destinationHost = "localhost";
            int destinationPort = 4318;
            int localport = 4319;
            // Printing a start-up message
            System.out.println("Starting proxy for " + destinationHost + ":" + destinationPort + " on port " + localport);
            // And start running the server
            runServer(destinationHost, destinationPort, localport); // never returns
        } catch (Exception e) {
            System.err.println(e); // Prints the standard errors
        }
    }

    public static void runServer(String host, int remoteport, int localport) throws IOException {
        // Creating a ServerSocket to listen for connections with
        ServerSocket s = new ServerSocket(localport);
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];
        while (true) {
          Socket client = s.accept();
          Thread t = new Thread("fromn server") {
              public void run() {
                  try {
                    runClient(client, host, remoteport);
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
          };
          t.start();
        }
    }
    public static void runClient(Socket client, String host, int remoteport) throws IOException {
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];
        Socket server = null;
        try {
            final InputStream streamFromClient = client.getInputStream();
            final OutputStream streamToClient = client.getOutputStream();

            // Create a connection to the real server.
            // If we cannot connect to the server, send an error to the
            // client, disconnect, and continue waiting for connections.
            try {
                server = new Socket(host, remoteport);

            } catch (IOException e) {
                System.out.println("Proxy server cannot connect to " + host + ":" + remoteport + ":\n" + e + "\n");
                PrintWriter out = new PrintWriter(streamToClient);
                out.print("Proxy server cannot connect to " + host + ":" + remoteport + ":\n" + e + "\n");
                out.flush();
                client.close();
                return;
            }

            // Get server streams.
            final InputStream streamFromServer = server.getInputStream();
            final OutputStream streamToServer = server.getOutputStream();

            // a thread to read the client's requests and pass them
            // to the server. A separate thread for asynchronous.
            Thread t = new Thread("to server") {
                public void run() {
                    int bytesRead;
                    try {
                        while ((bytesRead = streamFromClient.read(request)) != -1) {
                            String chunk = new String(request, 0, bytesRead);
                           if (chunk.contains("HostX: collector:4319")) {
                                chunk = chunk.replace("Host: collector:4319", "Host: localhost:4318");
                                byte[] request2 = chunk.getBytes();
                                streamToServer.write(request2, 0, request2.length);
                                streamToServer.flush();
                                System.out.print(new String(request, 0, bytesRead));
                            } else {
                                streamToServer.write(request, 0, bytesRead);
                                streamToServer.flush();
                                System.out.print(new String(request, 0, bytesRead));
                            }
                        }
                    } catch (IOException e) {
                       System.err.println("1 "+e); // Prints the standard errors
                    }

                    // the client closed the connection to us, so close our
                    // connection to the server.
                    try {
                        streamToServer.close();
                    } catch (IOException e) {
                       System.err.println("2 "+e); // Prints the standard errors
                    }
                }
            };

            // Start the client-to-server request thread running
            t.start();
            
            // Read the server's responses
            // and pass them back to the client.
            int bytesRead;
            try {
                while ((bytesRead = streamFromServer.read(reply)) != -1) {
                    streamToClient.write(reply, 0, bytesRead);
                    streamToClient.flush();
                }
            } catch (IOException e) {
                       System.err.println("3 "+e); // Prints the standard errors
            }
            // The server closed its connection to us, so we close our
            // connection to our client.
            streamToClient.close();
        } catch (IOException e) {
                       System.err.println("4 "+e); // Prints the standard errors
        } finally {
            try {
                if (server != null)
                    server.close();
                if (client != null)
                    client.close();
            } catch (IOException e) {
                       System.err.println("5 "+e); // Prints the standard errors
            }
        }
    
    }

}
