package com.example.demo.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.VoucherService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="spring.datasource.url=jdbc:h2:mem:adminvouchers;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AdminVoucherTests {
    @MockitoBean JavaMailSender mailSender;
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired VoucherRepository vouchers;
    @Autowired VoucherCampaignRepository campaigns;
    @Autowired VoucherService service;
    @Autowired PasswordEncoder encoder;
    @Autowired com.example.demo.service.AdminVoucherService adminVouchers;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    record Session(User user,HttpClient client,String token) implements AutoCloseable {
        public void close(){client.close();}
    }
    HttpResponse<String> request(HttpClient client,String path,String body,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(token!=null)builder.header("X-CSRF-TOKEN",token);
        if(body==null)builder.GET();else builder.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    String token(String html) {
        var m=Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html);
        assertThat(m.find()).isTrue();return m.group(1);
    }
    Session login(RoleName role) throws Exception {
        var u=new User();var name="gift-"+UUID.randomUUID();u.setUsername(name);u.setEmail(name+"@example.com");
        u.setFullName("Khách <script>alert(1)</script>");u.setPassword(encoder.encode("test-password"));
        u.addRole(roles.findByName(role).orElseGet(()->{var r=new Role();r.setName(role);return roles.saveAndFlush(r);}));users.saveAndFlush(u);
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf=token(request(client,"/login",null,null).body());
        assertThat(request(client,"/login","username="+name+"&password=test-password",csrf).statusCode()).isEqualTo(302);
        return new Session(u,client,token(request(client,"/",null,null).body()));
    }
    String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    @Test void adminCreatesPreviewsIssuesAndRevokesWhileCustomersHaveOwnWallet() throws Exception {
        try(var admin=login(RoleName.ROLE_ADMIN);var buyer=login(RoleName.ROLE_USER);var other=login(RoleName.ROLE_USER)) {
            assertThat(request(buyer.client(),"/admin/vouchers",null,null).statusCode()).isEqualTo(403);
            String input="name="+encode("Quà <script>alert(2)</script>")+"&discountType=FIXED&discountValue=50000&minimumTotal=300000&expiryDate="+LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(7);
            assertThat(request(admin.client(),"/admin/vouchers",input,null).statusCode()).isEqualTo(403);
            var invalid=request(admin.client(),"/admin/vouchers",input.replace("discountType=FIXED&discountValue=50000","discountType=PERCENT&discountValue=101"),admin.token());
            assertThat(invalid.statusCode()).isEqualTo(200);
            assertThat(invalid.body()).contains("100%");
            var expired=request(admin.client(),"/admin/vouchers",input.replaceAll("expiryDate=[^&]+","expiryDate=2020-01-01"),admin.token());
            assertThat(expired.statusCode()).isEqualTo(200);
            assertThat(expired.body()).contains("Hạn dùng đã hết");
            var created=request(admin.client(),"/admin/vouchers",input,admin.token());
            assertThat(created.statusCode()).isEqualTo(302);
            String location=URI.create(created.headers().firstValue("Location").orElseThrow()).getPath();
            long id=Long.parseLong(location.substring(location.lastIndexOf('/')+1));
            var detail=request(admin.client(),location,null,null);
            assertThat(detail.statusCode()).isEqualTo(200);
            assertThat(detail.body()).contains("&lt;script&gt;alert(2)&lt;/script&gt;","name=\"recipientIds\"")
                    .doesNotContain("<script>alert(2)</script>","<script>alert(1)</script>");
            var preview=request(admin.client(),location+"/random","count=2",admin.token());
            assertThat(preview.statusCode()).isEqualTo(200);
            assertThat(preview.body()).contains("Danh sách may mắn", "Tặng voucher cho 2 người này");
            assertThat(vouchers.findByCampaignIdOrderByIdDesc(id)).isEmpty();
            verifyNoInteractions(mailSender);
            assertThat(request(admin.client(),location+"/random","count=101",admin.token()).body()).contains("1 đến 100");
            var search=request(admin.client(),location+"?q="+encode(buyer.user().getEmail()),null,null);
            assertThat(search.body()).contains(buyer.user().getEmail()).doesNotContain(other.user().getEmail());
            String recipients="recipientIds="+buyer.user().getId()+"&recipientIds="+other.user().getId();
            assertThat(request(buyer.client(),location+"/issue",recipients,buyer.token()).statusCode()).isEqualTo(403);
            assertThat(request(admin.client(),location+"/issue",recipients,admin.token()).statusCode()).isEqualTo(302);
            assertThat(request(admin.client(),location+"/issue",recipients,admin.token()).statusCode()).isEqualTo(302);
            var issued=vouchers.findByCampaignIdOrderByIdDesc(id);assertThat(issued).hasSize(2);
            assertThat(issued).extracting(Voucher::getCode).doesNotHaveDuplicates();
            String buyerCode=service.available(buyer.user().getUsername()).getFirst();
            String otherCode=service.available(other.user().getUsername()).getFirst();
            var messages=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender,times(2)).send(messages.capture());
            for(var message:messages.getAllValues()) {
                assertThat(message.getSubject()).contains("voucher quà tặng");
                assertThat(message.getTo()).hasSize(1);
                String ownCode=message.getTo()[0].equals(buyer.user().getEmail())?buyerCode:otherCode;
                assertThat(message.getText()).contains(ownCode,"Giảm 50.000 VNĐ","300.000 VNĐ","Hạn sử dụng:","giờ Việt Nam","Mỗi mã dùng một lần");
                assertThat(message.getText()).doesNotContain(ownCode.equals(buyerCode)?otherCode:buyerCode);
            }
            var wallet=request(buyer.client(),"/orders",null,null);
            assertThat(wallet.statusCode()).isEqualTo(200);
            assertThat(wallet.body()).contains(buyerCode,"50.000").containsPattern("300[.,]000").doesNotContain(otherCode);
            assertThatThrownBy(()->service.preview(other.user().getUsername(),buyerCode,new java.math.BigDecimal("300000")))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            var gift=issued.stream().filter(v->v.getCode().equals(buyerCode)).findFirst().orElseThrow();
            assertThat(request(admin.client(),location+"/revoke/"+gift.getId(),"",admin.token()).statusCode()).isEqualTo(302);
            assertThat(service.available(buyer.user().getUsername())).isEmpty();
            assertThat(service.available(other.user().getUsername())).containsExactly(otherCode);
            assertThat(request(buyer.client(),"/orders",null,null).body()).contains("Đã thu hồi");
        }
    }
    @Test void rolledBackGiftsSendNothingAndSmtpFailureDoesNotUndoGiftsOrStopOtherRecipients() throws Exception {
        try(var admin=login(RoleName.ROLE_ADMIN);var buyer=login(RoleName.ROLE_USER);var other=login(RoleName.ROLE_USER)) {
            var form=new com.example.demo.dto.VoucherCampaignForm();form.setName("Quà phần trăm");
            form.setDiscountType("PERCENT");form.setDiscountValue(new java.math.BigDecimal("20"));
            form.setMinimumTotal(new java.math.BigDecimal("300000"));form.setMaximumDiscount(new java.math.BigDecimal("50000"));
            form.setExpiryDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(7));
            long id=adminVouchers.create(admin.user().getUsername(),form);
            var recipients=List.of(buyer.user().getId(),other.user().getId());
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx->{
                assertThat(adminVouchers.issue(admin.user().getUsername(),id,recipients)).isEqualTo(2);
                verifyNoInteractions(mailSender);
                tx.setRollbackOnly();
            });
            verifyNoInteractions(mailSender);
            assertThat(vouchers.findByCampaignIdOrderByIdDesc(id)).isEmpty();
            doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
            assertThat(adminVouchers.issue(admin.user().getUsername(),id,recipients)).isEqualTo(2);
            assertThat(vouchers.findByCampaignIdOrderByIdDesc(id)).hasSize(2);
            assertThat(service.available(buyer.user().getUsername())).hasSize(1);
            assertThat(service.available(other.user().getUsername())).hasSize(1);
            assertThat(adminVouchers.issue(admin.user().getUsername(),id,recipients)).isZero();
            var messages=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender,times(2)).send(messages.capture());
            assertThat(messages.getAllValues()).extracting(m->m.getTo()[0])
                    .containsExactlyInAnyOrder(buyer.user().getEmail(),other.user().getEmail());
            assertThat(messages.getAllValues()).allSatisfy(m->assertThat(m.getText())
                    .contains("Giảm 20%","Giảm tối đa: 50.000 VNĐ","Quà phần trăm"));
        }
    }
}
