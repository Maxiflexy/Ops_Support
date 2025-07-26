package util;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.FileUploadDbHelper;

import javax.jms.*;

public class MQUtil {

    final static Logger LOG = LogManager.getLogger(MQUtil.class);

    private static volatile MQUtil instance;
    private static Object mutex = new Object();

    // System exit status value (assume unset value to be 1)
    private static int status = 1;
    private static String HOST;
    private static int PORT;
    private static String CHANNEL;
    private static String QMGR;
    private static String APP_USER;
    private static String APP_PASSWORD;
    private static String QUEUE_NAME;

    private MQUtil() {
        HOST = System.getProperty("mq_host");
        PORT = Integer.parseInt(System.getProperty("mq_port"));
        CHANNEL = System.getProperty("mq_channel");
        QMGR = System.getProperty("mq_mgr");
        APP_USER = System.getProperty("mq_user");
        APP_PASSWORD = System.getProperty("mq_password");
        QUEUE_NAME = System.getProperty("mq_queue_name");

    }

    public static MQUtil getInstance() {
        MQUtil result = instance;
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new MQUtil();
            }
        }
        return result;
    }

    public void pushToQueue(String request) {
        JMSContext context = null;
        Destination destination = null;
        JMSProducer producer = null;

        try {
            // Create a connection factory
            JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            JmsConnectionFactory cf = ff.createConnectionFactory();

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
            cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
//            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Operation Support");
//            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
//            cf.setStringProperty(WMQConstants.USERID, APP_USER);
//            cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
            //cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");

            // Create JMS objects
            context = cf.createContext();
            destination = context.createQueue("queue:///" + QUEUE_NAME);

            TextMessage message = context.createTextMessage(request);

            producer = context.createProducer();
            producer.send(destination, message);
            System.out.println("Sent message:\n" + message);
        } catch (JMSException jmsex) {
            LOG.info("Error sending to queue", jmsex);
            recordFailure(jmsex);
        }
    }

    /**
     * Record this run as successful.
     */
    private static void recordSuccess() {
        System.out.println("SUCCESS");
        status = 0;
        return;
    }

    /**
     * Record this run as failure.
     *
     * @param ex
     */
    private static void recordFailure(Exception ex) {
        if (ex != null) {
            if (ex instanceof JMSException) {
                processJMSException((JMSException) ex);
            } else {
                System.out.println(ex);
            }
        }
        System.out.println("FAILURE");
        status = -1;
        return;
    }

    /**
     * Process a JMSException and any associated inner exceptions.
     *
     * @param jmsex
     */
    private static void processJMSException(JMSException jmsex) {
        System.out.println(jmsex);
        Throwable innerException = jmsex.getLinkedException();
        if (innerException != null) {
            System.out.println("Inner exception(s):");
        }
        while (innerException != null) {
            System.out.println(innerException);
            LOG.info("Exception: {}", innerException);
            innerException = innerException.getCause();
        }
        return;
    }
}
