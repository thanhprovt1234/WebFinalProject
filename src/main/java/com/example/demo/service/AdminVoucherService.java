package com.example.demo.service;

import com.example.demo.dto.VoucherCampaignForm;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class AdminVoucherService {
    private final VoucherCampaignRepository campaigns;
    private final VoucherRepository vouchers;
    private final UserRepository users;
    private final AccessService access;
    private final VoucherService voucherService;
    private final jakarta.validation.Validator validator;
    private final org.springframework.context.ApplicationEventPublisher events;
    public AdminVoucherService(VoucherCampaignRepository campaigns,VoucherRepository vouchers,UserRepository users,
            AccessService access,VoucherService voucherService,jakarta.validation.Validator validator,
            org.springframework.context.ApplicationEventPublisher events) {
        this.campaigns=campaigns;this.vouchers=vouchers;this.users=users;this.access=access;this.voucherService=voucherService;
        this.validator=validator;
        this.events=events;
    }
    private User admin(String actor) {
        var user=access.account(actor);
        if(!access.has(user,RoleName.ROLE_ADMIN)) throw new AccessDeniedException("Chỉ admin được quản lý voucher.");
        return user;
    }
    public org.springframework.data.domain.Page<VoucherCampaign> campaigns(String actor,int page) {
        admin(actor);return campaigns.findAllByOrderByIdDesc(PageRequest.of(Math.max(0,page),20));
    }
    public VoucherCampaign campaign(String actor,Long id) {
        admin(actor);return campaigns.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }
    @Transactional
    public Long create(String actor,@jakarta.validation.Valid VoucherCampaignForm form) {
        var creator=admin(actor);
        if(!validator.validate(form).isEmpty()) throw new IllegalArgumentException("Vui lòng nhập đầy đủ điều kiện voucher hợp lệ.");
        if("PERCENT".equals(form.getDiscountType()) && form.getDiscountValue().compareTo(new java.math.BigDecimal("100"))>0)
            throw new IllegalArgumentException("Mức giảm phần trăm không được vượt quá 100%.");
        var c=new VoucherCampaign();c.setName(form.getName());c.setDiscountType(form.getDiscountType());
        c.setDiscountValue(form.getDiscountValue());c.setMinimumTotal(form.getMinimumTotal());
        c.setMaximumDiscount("PERCENT".equals(form.getDiscountType())?form.getMaximumDiscount():null);
        c.setExpiresAt(form.getExpiryDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        if(!c.getExpiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Hạn dùng đã hết.");
        c.setCreatedBy(creator);return campaigns.save(c).getId();
    }
    public List<User> candidates(String actor,Long campaignId,String q) {
        admin(actor);
        var issued=new HashSet<Long>();
        vouchers.findByCampaignIdOrderByIdDesc(campaignId).forEach(v->issued.add(v.getUser().getId()));
        return users.findVoucherRecipients(q.strip()).stream().filter(u->!issued.contains(u.getId())).toList();
    }
    public List<User> random(String actor,Long campaignId,int count) {
        if(!campaign(actor,campaignId).isActive()) throw new IllegalArgumentException("Chương trình đã hết hạn.");
        if(count<1 || count>100) throw new IllegalArgumentException("Chọn từ 1 đến 100 người mỗi lần.");
        var pool=new ArrayList<>(candidates(actor,campaignId,""));
        if(count>pool.size()) throw new IllegalArgumentException("Không đủ khách hàng chưa nhận quà trong chương trình này.");
        Collections.shuffle(pool,new java.security.SecureRandom());return List.copyOf(pool.subList(0,count));
    }
    @Transactional
    public int issue(String actor,Long campaignId,List<Long> recipientIds) {
        var issuer=admin(actor);
        var c=campaigns.lockById(campaignId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy chương trình."));
        if(!c.getExpiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Chương trình đã hết hạn.");
        var ids=new LinkedHashSet<>(recipientIds);
        if(ids.isEmpty() || ids.size()>100) throw new IllegalArgumentException("Chọn từ 1 đến 100 người nhận.");
        var recipients=users.findAllById(ids);
        if(recipients.size()!=ids.size() || recipients.stream().anyMatch(u->!u.isEnabled() || !access.has(u,RoleName.ROLE_USER)))
            throw new IllegalArgumentException("Người nhận phải là khách hàng đang hoạt động.");
        int count=0;
        for(var u:recipients) {
            if(vouchers.existsByCampaignIdAndUserId(campaignId,u.getId())) continue;
            var v=new Voucher();v.setCode("GIFT-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT));
            v.setUser(u);v.setCampaign(c);v.setIssuedBy(issuer);vouchers.save(v);count++;
            events.publishEvent(new VoucherGifted(v.getId(),u.getEmail(),u.getFullName(),c.getName(),
                    v.getCode(),c.getDiscountType(),c.getDiscountValue(),c.getMinimumTotal(),
                    c.getMaximumDiscount(),c.getExpiryLabel()));
        }
        vouchers.flush();return count;
    }
    public List<Map<String,Object>> issued(String actor,Long id) {
        admin(actor);return vouchers.findByCampaignIdOrderByIdDesc(id).stream().map(v->Map.<String,Object>of(
                "id",v.getId(),"code",v.getCode(),"username",v.getUser().getUsername(),"email",v.getUser().getEmail(),
                "issuedBy",v.getIssuedBy()==null?"":v.getIssuedBy().getUsername(),
                "issuedAt",voucherService.date(v.getCreatedAt().toInstant(ZoneOffset.UTC)),
                "status",voucherService.status(v),"canRevoke",v.getUsedOrder()==null && v.getRevokedAt()==null)).toList();
    }
    @Transactional
    public void revoke(String actor,Long campaignId,Long voucherId) {
        var revoker=admin(actor);
        var v=vouchers.lockById(voucherId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy voucher."));
        if(v.getCampaign()==null || !v.getCampaign().getId().equals(campaignId)) throw new IllegalArgumentException("Voucher không thuộc chương trình.");
        if(v.getUsedOrder()!=null) throw new IllegalArgumentException("Không thể thu hồi voucher đã sử dụng.");
        if(v.getRevokedAt()==null){v.setRevokedAt(Instant.now());v.setRevokedBy(revoker);}
    }
}
