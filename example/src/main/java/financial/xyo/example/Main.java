package financial.xyo.example;

import financial.xyo.ClientConfig;
import financial.xyo.XyoClient;
import financial.xyo.XyoException;

public class Main {
    public static void main(String[] args) {
        try {
            // Instantiate the Client with a dummy API key configuration
            ClientConfig config = new ClientConfig("RandomBase64EncodedStringApiKey");
            XyoClient client = new XyoClient(config);

            System.out.println("Successfully imported and instantiated the XYO Client (Java)");
        } catch (XyoException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
