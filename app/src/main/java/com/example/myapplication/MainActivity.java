package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.database.DataManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.SQLException;
import java.util.Base64;


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
        EditText usernameForm = findViewById(R.id.editTextUsername);

        EditText passwordForm = findViewById(R.id.editTextTextPassword);

        EditText quantityFormCamas = findViewById(R.id.editQuantityCamas);

        EditText quantityFormMesas = findViewById(R.id.editQuantityMesas);

        EditText quantityFormSillas = findViewById(R.id.editQuantitySillas);

        EditText quantityFormSillon = findViewById(R.id.editQuantitySillon);

        if (validateNumbersFields(quantityFormCamas, quantityFormMesas, quantityFormSillas, quantityFormSillon) ||
                validateTextFields(usernameForm, passwordForm)) {
            // Mostramos un mensaje emergente;
            Toast.makeText(getApplicationContext(), "Comprueba los datos aportados", Toast.LENGTH_SHORT).show();
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

                                    Toast.makeText(MainActivity.this, "Petición registrada", Toast.LENGTH_SHORT).show();
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

        EditText usernameForm = findViewById(R.id.editTextUsername);
        EditText passwordForm = findViewById(R.id.editTextTextPassword);
        EditText quantityFormCamas = findViewById(R.id.editQuantityCamas);
        EditText quantityFormMesas = findViewById(R.id.editQuantityMesas);
        EditText quantityFormSillas = findViewById(R.id.editQuantitySillas);
        EditText quantityFormSillon = findViewById(R.id.editQuantitySillon);

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void run() {
            try {

                // Se hashea la clave para que pueda ser comprobada en la BBDD
                String username = usernameForm.getText().toString();
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashPwrd = md.digest(passwordForm.getText().toString().getBytes(StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                for (byte b : hashPwrd) {
                    sb.append(String.format("%02x", b));
                }
                String hash = sb.toString();

                // A partir del user y la password, se obtiene la clave publica almacenada
                String key = obtenerClavePublicaUser(username, hash);
                // En caso de que no se encuentre el user especificado, no se continua
                if (key == null) {
                    showMessage("El usuario y contraseña aportados no estan registrados");
                    return ;
                }

                // Conexión con el servidor
                System.out.println("Connecting to " + serverAddress + " on port " + serverPort + "...");
                Socket clientSocket = new Socket(serverAddress, serverPort);
                System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Generación del mensaje con el pedido
                String msg = "Camas:" + quantityFormCamas.getText().toString() + " "
                        + "Mesas:" + quantityFormMesas.getText().toString() + " "
                        + "Sillas:" + quantityFormSillas.getText().toString() + " "
                        + "Sillones:" + quantityFormSillon.getText().toString();

                // Se obtiene la clave privada a partir del fichero, dedodificandola.
                String privateKey = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    AssetManager assetManager = MainActivity.this.getAssets();
                    InputStream inputStream = assetManager.open("clavePrivada.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    privateKey = reader.readLine();

                }

                byte[] privateKeyBytes = new byte[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    privateKeyBytes = Base64.getDecoder().decode(privateKey);
                }
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey clavePrivada = keyFactory.generatePrivate(keySpec);

                // Se inicia el proceso de generación de firma
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(clavePrivada);
                sig.update(msg.getBytes());
                byte[] firma = sig.sign();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    out.println(username + "@" + msg + "@" + Base64.getEncoder().encodeToString(firma));
                }

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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        private void showMessage(String message) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String obtenerClavePublicaUser(String username, String hashPwrd) throws SQLException {
            // Se accede a la DDBB para obtener la clave publica del usuario
            DataManager dataManager = new DataManager(MainActivity.this);
            dataManager.open();
            String publicKeyUser = dataManager.getData(username, hashPwrd);
            if (publicKeyUser == null) {
                System.out.println("ERROR DURANTE EL INICIO DE SESION");
            }
            dataManager.close();

            return publicKeyUser;
        }
    }

    private boolean validateNumbersFields(EditText quantityFormCamas, EditText quantityFormMesas, EditText quantityFormSillas, EditText quantityFormSillon) {

        boolean quantitiesNotOverLimitCamas = quantityFormCamas.getText().toString().isEmpty() || Integer.parseInt(quantityFormCamas.getText().toString()) > 300;
        boolean quantitiesNotOverLimitMesas = quantityFormMesas.getText().toString().isEmpty() || Integer.parseInt(quantityFormMesas.getText().toString()) > 300;
        boolean quantitiesNotOverLimitSillas = quantityFormSillas.getText().toString().isEmpty() || Integer.parseInt(quantityFormSillas.getText().toString()) > 300;
        boolean quantitiesNotOverLimitSillon = quantityFormSillon.getText().toString().isEmpty() || Integer.parseInt(quantityFormSillon.getText().toString()) > 300;

        boolean quantitiesNotEmptyAndOverLimit = quantitiesNotOverLimitCamas
                || quantitiesNotOverLimitMesas
                || quantitiesNotOverLimitSillas
                || quantitiesNotOverLimitSillon;

        boolean inputValidation = !quantityFormCamas.getText().toString().matches("^[0-9]+$")
                || !quantityFormMesas.getText().toString().matches("^[0-9]+$")
                || !quantityFormSillas.getText().toString().matches("^[0-9]+$")
                || !quantityFormSillon.getText().toString().matches("^[0-9]+$");

        return quantitiesNotEmptyAndOverLimit || inputValidation;
    }

    private boolean validateTextFields(EditText usernameForm, EditText passwordForm) {
        boolean notEmpty = usernameForm.getText().toString().isEmpty()
                || passwordForm.getText().toString().isEmpty();

        boolean inputValidation = !usernameForm.getText().toString().matches("^[a-zA-Z0-9 ]+$")
                || !passwordForm.getText().toString().matches("^[a-zA-Z0-9 ]+$");

        return notEmpty || inputValidation;
    }

}
