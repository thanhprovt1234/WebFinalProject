package com.example.demo.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Role removals and account disabling take effect on the next request, including existing sessions. */
public class CurrentRolesFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService users;
    public CurrentRolesFilter(CustomUserDetailsService users) { this.users=users; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().matches("^/(css|js|images)/.*");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var previous=SecurityContextHolder.getContext().getAuthentication();
        if(previous!=null && previous.isAuthenticated() && !(previous instanceof AnonymousAuthenticationToken)) {
            try {
                var current=users.loadUserByUsername(previous.getName());
                if(!current.isEnabled()) throw new UsernameNotFoundException("Account disabled");
                var refreshed=UsernamePasswordAuthenticationToken.authenticated(current,null,current.getAuthorities());
                refreshed.setDetails(previous.getDetails());
                SecurityContextHolder.getContext().setAuthentication(refreshed);
            } catch(UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
                var session=request.getSession(false);if(session!=null) session.invalidate();
            }
        }
        chain.doFilter(request,response);
    }
}
