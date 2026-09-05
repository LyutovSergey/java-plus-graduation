package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserRecommendationsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("getRecommendationsForUser: userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> recommendations =
                    recommendationService.getRecommendationsForUser(
                            request.getUserId(),
                            request.getMaxResults()
                    );

            for (RecommendedEventProto rec : recommendations) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting recommendations for user {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("getSimilarEvents: eventId={}, userId={}, maxResults={}",
                request.getEventId(), request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> similarEvents =
                    recommendationService.getSimilarEvents(
                            request.getEventId(),
                            request.getUserId(),
                            request.getMaxResults()
                    );

            for (RecommendedEventProto rec : similarEvents) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting similar events for event {}", request.getEventId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("getInteractionsCount: eventIds={}", request.getEventIdList());

        try {
            List<RecommendedEventProto> interactions =
                    recommendationService.getInteractionsCount(
                            request.getEventIdList()
                    );

            for (RecommendedEventProto rec : interactions) {
                responseObserver.onNext(rec);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting interactions count", e);
            responseObserver.onError(e);
        }
    }
}