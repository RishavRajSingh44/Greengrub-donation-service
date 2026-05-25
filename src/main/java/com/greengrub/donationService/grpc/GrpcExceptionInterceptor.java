package com.greengrub.donationService.grpc;

import com.greengrub.donationService.exception.DonationNotFoundException;
import com.greengrub.donationService.exception.FoodServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.dao.DataAccessException;

@Slf4j
@GrpcGlobalServerInterceptor
public class GrpcExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    handleException(e, call, headers);
                }
            }

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    handleException(e, call, headers);
                }
            }
        };
    }

    private <ReqT, RespT> void handleException(Exception e, ServerCall<ReqT, RespT> call, Metadata headers) {
        Status status = mapToGrpcStatus(e);
        log.error("[gRPC] {} on {}: {}", status.getCode(), call.getMethodDescriptor().getFullMethodName(), e.getMessage(), e);
        call.close(status, headers);
    }

    private Status mapToGrpcStatus(Exception e) {
        if (e instanceof DonationNotFoundException ex)
            return Status.NOT_FOUND.withDescription(ex.getMessage());
        if (e instanceof IllegalArgumentException ex)
            return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
        if (e instanceof ConstraintViolationException ex)
            return Status.INVALID_ARGUMENT.withDescription(
                ex.getConstraintViolations().iterator().next().getMessage());
        if (e instanceof FoodServiceException)
            return Status.UNAVAILABLE.withDescription("food-service temporarily unavailable — please retry");
        if (e instanceof CallNotPermittedException)
            return Status.UNAVAILABLE.withDescription("Service temporarily unavailable — please retry");
        if (e instanceof DataAccessException)
            return Status.UNAVAILABLE.withDescription("Database temporarily unavailable");
        return Status.INTERNAL.withDescription("Internal server error: " + e.getMessage());
    }
}
