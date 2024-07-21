package uom.cse;

import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
    private List<Neighbour> neighbours;
    private String nodeIp;
    private int nodePort;
    private String username;
    private DatagramSocket socket;

    public Node(String nodeIp, int nodePort, String username) throws SocketException {
        this.nodeIp = nodeIp;
        this.nodePort = nodePort;
        this.username = username;
        neighbours = new ArrayList<>();
        socket = new DatagramSocket(nodePort);
    }

    public void addNeighbour(String nodeIp, int nodePort) {
        neighbours.add(new Neighbour(nodeIp, nodePort, ""));
    }

    public void registerWithBS(String bsIp, int bsPort) throws IOException {
        String message = String.format("%04d REG %s %d %s", 9 + nodeIp.length() + username.length(), nodeIp, nodePort, username);
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(bsIp), bsPort);
        socket.send(packet);

        buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received from BS: " + response);

        //add neighbors
        if (response != null && response.contains("REGOK")) {
            String[] responseParts = response.split(" ");
            int numNodes = Integer.parseInt(responseParts[2]);

            for (int i = 0; i < numNodes; i++) {
                String nodeIp = responseParts[3 + (i * 2)];
                int nodePort = Integer.parseInt(responseParts[4 + (i * 2)]);
                addNeighbour(nodeIp, nodePort);
            }
        }
    }

    public void searchFile(String fileName) throws IOException {
        String message = String.format("SEARCH %s", fileName);
        byte[] buffer = message.getBytes();

        System.out.println( "Number of Neighbors of node username : " + username + " port : " + nodePort + " is " + neighbours.size());

        for (Neighbour neighbour : neighbours) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(neighbour.getIp()), neighbour.getPort());
            socket.send(packet);
        }

        System.out.println("search requests sent from " + this.nodeIp + " " + this.nodePort);
        buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received: " + response);
    }

    public void downloadFile(String fileName, Neighbour neighbour) throws IOException {
        String url = String.format("http://%s:%d/download?file=%s", neighbour.getIp(), neighbour.getPort(), fileName);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] fileData = outputStream.toByteArray();
            System.out.println("Downloaded file: " + fileName);
            System.out.println("File size: " + fileData.length + " bytes");
        } else {
            System.out.println("Failed to download file: " + responseCode);
        }
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter file name to search or 'exit' to quit: ");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                searchFile(input);

                System.out.print("Download file? (y/n): ");
                String download = scanner.nextLine();

                if (download.equalsIgnoreCase("y")) {
                    // Assuming the first neighbour has the file for simplicity
                    if (!neighbours.isEmpty()) {
                        downloadFile(input, neighbours.get(0));
                    } else {
                        System.out.println("No neighbours found.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java Node <Node IP> <Node Port> <Username> <BS IP> <BS Port>");
            return;
        }

        try {
            String nodeIp = args[0];
            int nodePort = Integer.parseInt(args[1]);
            String username = args[2];
            String bsIp = args[3];
            int bsPort = Integer.parseInt(args[4]);

            Node node = new Node(nodeIp, nodePort, username);
            node.registerWithBS(bsIp, bsPort);
            node.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
