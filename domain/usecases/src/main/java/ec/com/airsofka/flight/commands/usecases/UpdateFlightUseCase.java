package ec.com.airsofka.flight.commands.usecases;

import ec.com.airsofka.aggregate.flightOperation.FlightOperation;
import ec.com.airsofka.aggregate.flightOperation.events.EventsFlighOperationEnum;
import ec.com.airsofka.aggregate.flightOperation.events.FlightCreated;
import ec.com.airsofka.flight.commands.UpdateFlightCommand;
import ec.com.airsofka.flight.queries.responses.FlightResponse;
import ec.com.airsofka.gateway.BusEvent;
import ec.com.airsofka.gateway.IEventStore;
import ec.com.airsofka.generics.domain.DomainEvent;
import ec.com.airsofka.generics.interfaces.IUseCaseExecute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UpdateFlightUseCase implements IUseCaseExecute<UpdateFlightCommand, FlightResponse> {
    private final IEventStore repository;
    private final BusEvent busEvent;

    public UpdateFlightUseCase(IEventStore repository, BusEvent busEvent) {
        this.repository = repository;
        this.busEvent = busEvent;
    }

    @Override
    public Mono<FlightResponse> execute(UpdateFlightCommand cmd) {
        Mono<FlightCreated> flightCreatedEvent = repository.findAllAggregateByEvent("flightoperation", EventsFlighOperationEnum.FLIGHT_CREATED.name())
                .map(event -> (FlightCreated) event)
                .filter(event -> event.getId().equals(cmd.getId()))
                .single();

        return flightCreatedEvent.flatMap(flightUpdated -> {
            Flux<DomainEvent> flightEvents = repository.findAggregate(flightUpdated.getAggregateRootId(), "flightoperation");

            return FlightOperation.from(flightUpdated.getAggregateRootId(), flightEvents)
                    .flatMap(flight -> {
                        flight.updateFlight(
                                flightUpdated.getId(),
                                flightUpdated.getOrigin(),
                                flightUpdated.getDestination(),
                                flightUpdated.getDeparture(),
                                flightUpdated.getArrival(),
                                flightUpdated.getPrice(),
                                flightUpdated.getIdPlane());

                        // Guardar los eventos no comprometidos de forma reactiva
                        return Flux.fromIterable(flight.getUncommittedEvents())
                                .flatMap(repository::save) // Guardar cada evento en el EventStore
                                .doOnNext( updateEvent -> busEvent.sendEventFlightUpdated(Mono.just(updateEvent))) // Enviar eventos a través de BusEvent
                                .then(Mono.defer(() -> {
                                    // Marcar los eventos como comprometidos
                                    flight.markEventsAsCommitted();

                                    // Crear y devolver la respuesta
                                    return Mono.just(new FlightResponse(
                                            flight.getFlight().getId().getValue(),
                                            flight.getFlight().getOrigin().getValue(),
                                            flight.getFlight().getDestination().getValue(),
                                            flight.getFlight().getDeparture().getValue(),
                                            flight.getFlight().getArrival().getValue(),
                                            flight.getFlight().getPrice().getValue(),
                                            flight.getFlight().getIdPlane().getValue()
                                    ));
                                }));
                    });
        });
    }
}
