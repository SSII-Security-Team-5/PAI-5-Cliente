package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String server = "192.168.1.12";
    protected static int port = 7070;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Capturamos el boton de Enviar
        View button = findViewById(R.id.button_send);

        // Llama al listener del boton Enviar
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });


    }

    // Creación de un cuadro de dialogo para confirmar pedido
    private void showDialog() throws Resources.NotFoundException {
        EditText quantityFormCamas = (EditText) findViewById(R.id.editQuantityCamas);

        EditText quantityFormMesas = (EditText) findViewById(R.id.editQuantityMesas);

        EditText quantityFormSillas = (EditText) findViewById(R.id.editQuantitySillas);

        EditText quantityFormSillon = (EditText) findViewById(R.id.editQuantitySillon);

        if (validateNumbersFields(quantityFormCamas, quantityFormMesas, quantityFormSillas, quantityFormSillon)) {
            // Mostramos un mensaje emergente;
            Toast.makeText(getApplicationContext(), "Selecciona al menos un elemento", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Enviar")
                    .setMessage("Se va a proceder al envio")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                // Catch ok button and send information
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    Thread t = new Thread(new ClientThread(server, port));
                                    t.start();

                                    // 1. Extraer los datos de la vista

                                    // 2. Firmar los datos

                                    // 3. Enviar los datos

                                    Toast.makeText(MainActivity.this, "Petición enviada correctamente", Toast.LENGTH_SHORT).show();
                                }
                            }

                    )
                    .

                            setNegativeButton(android.R.string.no, null)

                    .

                            show();
        }
    }

    public class ClientThread extends Thread {

        private String serverAddress;
        private int serverPort;

        public ClientThread(String serverAddress, int serverPort) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        EditText quantityFormCamas = (EditText) findViewById(R.id.editQuantityCamas);

        EditText quantityFormMesas = (EditText) findViewById(R.id.editQuantityMesas);

        EditText quantityFormSillas = (EditText) findViewById(R.id.editQuantitySillas);

        EditText quantityFormSillon = (EditText) findViewById(R.id.editQuantitySillon);

        public void run() {
            try {
                System.out.println("Connecting to " + serverAddress + " on port " + serverPort + "...");
                Socket clientSocket = new Socket(serverAddress, serverPort);
                System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String msg = "Camas: " + quantityFormCamas.getText().toString();

                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                PublicKey clavePublica = pair.getPublic();
                PrivateKey clavePrivada = pair.getPrivate();

                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(clavePrivada);
                sig.update(msg.getBytes());
                byte[] firma = sig.sign();

                out.println(msg + "@" + firma + "@" + Base64.encode(clavePublica.getEncoded(), 0));

                // Clean up
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean validateNumbersFields(EditText quantityFormCamas, EditText quantityFormMesas, EditText quantityFormSillas, EditText quantityFormSillon) {
        boolean result = quantityFormCamas.getText().length() == 0
                && quantityFormMesas.getText().length() == 0
                && quantityFormSillas.getText().length() == 0
                && quantityFormSillon.getText().length() == 0;

        return result;
    }


}
