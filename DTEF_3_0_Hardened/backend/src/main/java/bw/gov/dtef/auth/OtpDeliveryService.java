package bw.gov.dtef.auth;

public interface OtpDeliveryService {
    void deliver(String destination, String code, String purpose);
}
