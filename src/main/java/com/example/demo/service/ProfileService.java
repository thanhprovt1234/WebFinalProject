package com.example.demo.service;

import com.example.demo.dto.AddressRequest;
import com.example.demo.dto.ProfileRequest;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    private final UserRepository users;
    private final AddressRepository addresses;
    public ProfileService(UserRepository users, AddressRepository addresses) {
        this.users = users;
        this.addresses = addresses;
    }

    public record Account(String username, String email, String fullName, String phone) {}
    @Transactional(readOnly = true)
    public Account account(String username) {
        var user = user(username, false);
        return new Account(user.getUsername(), user.getEmail(), user.getFullName(), user.getPhone());
    }

    @Transactional(readOnly = true)
    public List<Address> addresses(String username) {
        return addresses.findByUserIdOrderByIdAsc(user(username, false).getId());
    }

    @Transactional(readOnly = true)
    public AddressRequest address(String username, Long id) {
        var address = owned(user(username, false).getId(), id);
        var form = new AddressRequest();
        form.setReceiverName(address.getReceiverName()); form.setPhone(address.getPhone());
        form.setAddressLine(address.getAddressLine()); form.setWard(address.getWard());
        form.setDistrict(address.getDistrict()); form.setCity(address.getCity());
        form.setDefaultAddress(address.isDefault());
        return form;
    }

    @Transactional
    public void update(String username, ProfileRequest form) {
        var user = user(username, true);
        user.setFullName(form.getFullName());
        user.setPhone(form.getPhone());
    }

    @Transactional
    public void saveAddress(String username, Long id, AddressRequest form) {
        var user = user(username, true);
        var address = id == null ? new Address() : owned(user.getId(), id);
        address.setUser(user);
        address.setReceiverName(form.getReceiverName()); address.setPhone(form.getPhone());
        address.setAddressLine(form.getAddressLine()); address.setWard(form.getWard());
        address.setDistrict(form.getDistrict()); address.setCity(form.getCity());
        var existing = addresses.findByUserIdOrderByIdAsc(user.getId());
        // The first address is default. Editing an existing default retains it unless
        // another address is explicitly selected. Locking the account serializes writes.
        boolean makeDefault = form.isDefaultAddress() || address.isDefault()
                || existing.stream().noneMatch(Address::isDefault);
        if (makeDefault) existing.forEach(entry -> entry.setDefault(false));
        address.setDefault(makeDefault);
        addresses.save(address);
    }

    @Transactional
    public void makeDefault(String username, Long id) {
        var user = user(username, true);
        var selected = owned(user.getId(), id);
        addresses.findByUserIdOrderByIdAsc(user.getId()).forEach(address -> address.setDefault(false));
        selected.setDefault(true);
    }

    @Transactional
    public void deleteAddress(String username, Long id) {
        var user = user(username, true);
        var selected = owned(user.getId(), id);
        addresses.delete(selected);
        addresses.flush();
        if (selected.isDefault()) {
            addresses.findByUserIdOrderByIdAsc(user.getId()).stream().findFirst().ifPresent(address -> address.setDefault(true));
        }
    }

    private Address owned(Long userId, Long id) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private User user(String username, boolean lock) {
        return (lock ? users.findForProfileUpdate(username) : users.findByUsername(username)).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
