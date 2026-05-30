package com.greengrub.donationService.grpc;

import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.exception.FoodServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrpcExceptionInterceptorTest {

    GrpcExceptionInterceptor interceptor;

    @Mock ServerCall<Object, Object> call;
    @Mock ServerCallHandler<Object, Object> next;
    @Mock ServerCall.Listener<Object> delegate;

    Metadata headers = new Metadata();

    @BeforeEach
    void setUp() {
        interceptor = new GrpcExceptionInterceptor();
    }

    @Test
    void interceptCall_noException_passesThrough() {
        when(next.startCall(call, headers)).thenReturn(delegate);
        ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);
        listener.onHalfClose();
        verify(delegate).onHalfClose();
    }

    @Test
    void onHalfClose_donationNotFoundException_closesWithNotFound() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                new DonationNotFoundException("id-1")));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.NOT_FOUND);
    }

    @Test
    void onHalfClose_illegalArgumentException_closesWithInvalidArgument() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                new IllegalArgumentException("bad arg")));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void onHalfClose_constraintViolationException_closesWithInvalidArgument() {
        stubMethodDescriptor();
        jakarta.validation.ConstraintViolation<?> cv = mock(jakarta.validation.ConstraintViolation.class);
        when(cv.getMessage()).thenReturn("must not be blank");
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(cv.getPropertyPath()).thenReturn(path);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(ex));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void onHalfClose_foodServiceException_closesWithUnavailable() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                new FoodServiceException("down", new RuntimeException())));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.UNAVAILABLE);
    }

    @Test
    void onHalfClose_circuitBreakerOpen_closesWithUnavailable() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                CallNotPermittedException.createCallNotPermittedException(
                        io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test"))));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.UNAVAILABLE);
    }

    @Test
    void onHalfClose_dataAccessException_closesWithUnavailable() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                new DataAccessResourceFailureException("DB down")));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.UNAVAILABLE);
    }

    @Test
    void onHalfClose_genericException_closesWithInternal() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(exceptionDelegate(
                new RuntimeException("unexpected")));

        interceptor.interceptCall(call, headers, next).onHalfClose();

        verifyCallClosedWithCode(Status.Code.INTERNAL);
    }

    @Test
    void onMessage_exceptionFromDelegate_handledAndCallClosed() {
        stubMethodDescriptor();
        when(next.startCall(call, headers)).thenReturn(messageExceptionDelegate(
                new DonationNotFoundException("id-1")));

        interceptor.interceptCall(call, headers, next).onMessage(new Object());

        verifyCallClosedWithCode(Status.Code.NOT_FOUND);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubMethodDescriptor() {
        when(call.getMethodDescriptor()).thenReturn(
                MethodDescriptor.<Object, Object>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName("donation.DonationService/test")
                        .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
                        .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
                        .build());
    }

    private ServerCall.Listener<Object> exceptionDelegate(Exception ex) {
        return new ServerCall.Listener<>() {
            @Override public void onHalfClose() {
                throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
            }
        };
    }

    private ServerCall.Listener<Object> messageExceptionDelegate(Exception ex) {
        return new ServerCall.Listener<>() {
            @Override public void onMessage(Object msg) {
                throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
            }
        };
    }

    private void verifyCallClosedWithCode(Status.Code expectedCode) {
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any(Metadata.class));
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(expectedCode);
    }
}
