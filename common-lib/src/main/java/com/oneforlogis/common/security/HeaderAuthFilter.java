package com.oneforlogis.common.security;

import com.oneforlogis.common.model.Role;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class HeaderAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userName = request.getHeader("X-User-Name");
        String roleKey = request.getHeader("X-User-Role");

        if (userId != null && roleKey != null) {
            try {
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleKey));

                Role role = Role.fromKey(roleKey);

                UserPrincipal principal = new UserPrincipal(UUID.fromString(userId), userName, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role key: " + roleKey, e);
            }
        }

        filterChain.doFilter(request, response);
    }
}