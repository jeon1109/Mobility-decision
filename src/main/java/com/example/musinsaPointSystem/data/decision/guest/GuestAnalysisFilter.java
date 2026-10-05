package com.example.musinsaPointSystem.data.decision.guest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.musinsaPointSystem.common.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GuestAnalysisFilter extends OncePerRequestFilter {
	private static final int MAX_BYTES = 16 * 1024;
	private final GuestAdmissionService admission;
	private final ObjectMapper mapper;

	public GuestAnalysisFilter(GuestAdmissionService admission, ObjectMapper mapper) {
		this.admission = admission;
		this.mapper = mapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !"/v1/mobility/guest-decision-cases".equals(
			request.getServletPath());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws
		ServletException,
		IOException {
		try (var lease = admission.acquire(request.getRemoteAddr())) {
			if (request.getContentLengthLong() > MAX_BYTES) {
				tooLarge(response);
				return;
			}
			byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
			if (body.length > MAX_BYTES) {
				tooLarge(response);
				return;
			}
			chain.doFilter(new HttpServletRequestWrapper(request) {
				@Override
				public ServletInputStream getInputStream() {
					var input = new ByteArrayInputStream(body);
					return new ServletInputStream() {
						public int read() {
							return input.read();
						}

						public boolean isFinished() {
							return input.available() == 0;
						}

						public boolean isReady() {
							return true;
						}

						public void setReadListener(ReadListener listener) {
							throw new UnsupportedOperationException("Synchronous JSON request");
						}
					};
				}

				@Override
				public BufferedReader getReader() {
					return new BufferedReader(
						new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
				}
			}, response);
		} catch (GuestAdmissionException e) {
			response.setHeader("Retry-After", String.valueOf(e.retryAfter()));
			write(response, e.status(), e.code(), e.getMessage());
		} catch (ServletException | IOException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException("Guest request failed", e);
		}
	}

	private void tooLarge(HttpServletResponse response) throws IOException {
		write(response, 413, "REQUEST_TOO_LARGE", "요청 본문은 16KB 이하여야 합니다.");
	}

	private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getWriter(), ApiErrorResponse.of(code, message));
	}
}
