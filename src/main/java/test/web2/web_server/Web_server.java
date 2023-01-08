/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package test.web2.web_server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import test.web2.web_server.Server.Bind;

/**
 *
 * @author user0
 */
public class Web_server {

    public static void main(String[] args)
        throws
            IOException,
            URISyntaxException
    {
//        var index = Web_server.class.getResource("index.html");
        var index = Paths.get("src/main/resources/index.html");
        Server s = new Server(
                (short)8080,
                new Bind[] {
                    new Bind(index, "/index.html"),
                    new Bind(index, "/index"),
                    new Bind(index, "/")
                }
        );
        System.out.println("Server created");
        s.start();
        try {
            s.stop();
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
        s.start();
        while(System.in.read() != 'q');
        try {
            s.stop();
        } catch (InterruptedException ex) {
            
            ex.printStackTrace(System.err);
        }
    }
}
