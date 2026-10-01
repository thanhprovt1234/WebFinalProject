package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class AdminAccessService {
    private final UserRepository users;private final RoleRepository roles;private final StoreRepository stores;
    private final StoreManagerRepository managers;private final AccessService access;
    public AdminAccessService(UserRepository users,RoleRepository roles,StoreRepository stores,StoreManagerRepository managers,AccessService access){
        this.users=users;this.roles=roles;this.stores=stores;this.managers=managers;this.access=access;
    }
    public Page<Map<String,Object>> users(String query,int page) {
        return users.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(query.strip(),query.strip(),
                PageRequest.of(Math.max(0,Math.min(page,10000)),20,Sort.by("id"))).map(this::row);
    }
    private Map<String,Object> row(User user) {
        return Map.of("id",user.getId(),"username",user.getUsername(),"fullName",user.getFullName(),"enabled",user.isEnabled(),
                "roles",user.getRoles().stream().map(r->r.getName().name()).sorted().toList());
    }
    public Map<String,Object> user(Long id) {
        var user=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var assignments=managers.findByUserId(id).stream().map(m->Map.<String,Object>of("id",m.getStore().getId(),"name",m.getStore().getName(),"shop",m.getStore().getShop().getName())).toList();
        return Map.of("user",row(user),"assignments",assignments);
    }
    public List<Map<String,Object>> stores() {
        return stores.findAll(Sort.by("id")).stream().map(s->Map.<String,Object>of("id",s.getId(),"name",s.getName(),"shop",s.getShop().getName())).toList();
    }
    @Transactional
    public void updateRoles(String actor,Long targetId,Set<RoleName> selected) {
        // Serialize privilege changes, then re-check the actor against current DB state.
        roles.lockRole(RoleName.ROLE_ADMIN).orElseThrow();
        var admin=access.account(actor);
        if(!access.has(admin,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Quyền quản trị đã thay đổi.");
        var target=users.findById(targetId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var desired=EnumSet.noneOf(RoleName.class);desired.addAll(selected);desired.add(RoleName.ROLE_USER);
        if(target.getId().equals(admin.getId())&&!desired.contains(RoleName.ROLE_ADMIN))
            throw new IllegalArgumentException("Bạn không thể tự bỏ quyền Admin của mình.");
        if(target.isEnabled()&&access.has(target,RoleName.ROLE_ADMIN)&&!desired.contains(RoleName.ROLE_ADMIN)
                &&users.countEnabledRole(RoleName.ROLE_ADMIN)<=1)
            throw new IllegalArgumentException("Hệ thống cần ít nhất một Admin đang hoạt động.");
        for(var role:new ArrayList<>(target.getRoles()))if(!desired.contains(role.getName()))target.removeRole(role);
        for(var role:desired)target.addRole(roles.findByName(role).orElseThrow());
        if(!desired.contains(RoleName.ROLE_MANAGER))managers.deleteAll(managers.findByUserId(targetId));
    }
    @Transactional
    public void assignStore(String actor,Long targetId,Long storeId,boolean assigned) {
        roles.lockRole(RoleName.ROLE_ADMIN).orElseThrow();
        if(!access.has(access.account(actor),RoleName.ROLE_ADMIN))throw new AccessDeniedException("Quyền quản trị đã thay đổi.");
        var user=users.findById(targetId).filter(User::isEnabled).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var store=stores.findById(storeId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!assigned){managers.findByUserIdAndStoreId(targetId,storeId).ifPresent(managers::delete);return;}
        if(!access.has(user,RoleName.ROLE_MANAGER))throw new IllegalArgumentException("Cần cấp vai trò Manager trước khi phân công chi nhánh.");
        if(!managers.existsByUserIdAndStoreId(targetId,storeId)) {
            var assignment=new StoreManager();assignment.setUser(user);assignment.setStore(store);managers.save(assignment);
        }
    }
}
