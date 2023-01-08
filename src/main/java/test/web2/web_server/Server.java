/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.web2.web_server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author user0
 */
public class Server {
    private volatile Executor pool;
    private Thread main;
    private final Bind[] binds;
    private volatile ServerSocket serv_socket;
    private final short port;
    
    private Path err_4xx;
    private Path err_5xx;
    
    {
        err_4xx = Paths.get("src/main/resources/4xx.html");
        err_5xx = Paths.get("src/main/resources/5xx.html");
    }
    
    public static class Bind
    {
        private final Path file;
        private String location;
        
//        public Bind(Worker w, String loc) throws URISyntaxException
//        {
//            work = w;
//            location = new URI(loc);
//        }
        
        public Bind(Path file, String loc) throws FileNotFoundException
        {
            this.file = file;
            File f = file.toFile();
            if (f.isDirectory() || !f.exists())
                throw new FileNotFoundException();
            location = loc;
        }
        
        public Path getFilepath()
        {
            return file;
        }
        public String getLocation()
        {
            return location;
        }
        
        public boolean perform(String addr)
        {
            String[] req_loc = addr.split("\\?");
            return req_loc[0].equals(location);
                
                
        }
    }
    
    public Server(short port, Bind[] binds) throws IOException
    {
        this.port = port;
        this.binds = binds;
        pool = Executors.newCachedThreadPool();
    }
    
    public void start()
    {
        try {            
            serv_socket = new ServerSocket(port);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        main = new Thread(()->new_client());
        main.start();
    }
    
    private void http(Socket sock)
    {
        try (
            var bw = new BufferedWriter(
                new OutputStreamWriter(
                    sock.getOutputStream()
                )
            );
            var br = new BufferedReader(
                new InputStreamReader(
                    sock.getInputStream()
                )
            )
        ) {
            var proto_header = br.readLine();
            if (proto_header == null)
            {
                return;
            }
            var req_info = proto_header.split("\s");
            String
                method = req_info[0],
                page = req_info[1],
                ver = req_info[2];
            
            boolean has = false;
            
            String req = "";
            while (!req.contains("\n\r"))
            {
                req += (char)br.read();
            }
            
            String s = "";
            
            Map<String, String> values = new HashMap<>();
            values.put("method", method);
            values.put("loc", page);
            values.put("ver", ver);
            values.put("req", req);
            values.put("code", "" + 200);            
            
            for (Bind bind : binds) {
                if(bind.perform(page))
                {
                    s = Files.readString(
                        bind.getFilepath()
                    );
                    
                    var keys = values.keySet().toArray();
                    
                    for (Object key : keys) {
                        s = s.replaceAll("\\$\\{"+key+"\\}", values.get(key));
                    }
                    
                    has = true;
                    break;
                }
            }
            if (!has)
            {
                String key = "code";
                s = Files.readString(
                        this.err_4xx
                    ).replaceAll("\\$\\{"+key+"\\}", "" + 404);
                System.out.println("Client rejected (4xx)");
            }
            
            
            bw.write(
                http_send(s)    
            );
            
            bw.flush();
            
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        finally{
            try {
                sock.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
    
    private String http_send(String msg)
    {
        return String.format(
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n" +
            "Connection: keep-alive\r\n" +
            "Content-Length: %d\r\n\r\n" +
            "%s",
        msg.length(), msg);
    }
    
    private void new_client()
    {
        System.out.println("Server started");
        while(!serv_socket.isClosed())
        {    
            try {
                Socket client = serv_socket.accept();
                System.out.println("New client");
                try {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            http(client);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    var bw = new BufferedWriter(
                        new OutputStreamWriter(
                        client.getOutputStream()
                        )
                    );
                    bw.write(
                        http_send(
                            Files.readString(err_5xx)
                        )
                    );
                    bw.close();
                    client.close();
                    System.out.println("Client rejected (5xx)");
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            System.out.println("Client rejected");                    
        }
        System.out.println("Server stopping");
    }
    
    public void stop() throws IOException, InterruptedException
    {
        this.serv_socket.close();
//        this.pool = null;
        this.main.join();
        System.out.println("Server stopped");
    }
}
