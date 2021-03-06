
@Path("/")
@Singleton
public class EventResource {
 
    private final Logger logger = LoggerFactory.getLogger(EventResource.class);
 
    private EventListener listener;
    private EventGenerator generator;
 
    private @Context BroadcasterFactory bf;
 
    /**
     * Programmatic way to get a Broadcaster instance
     * @return the MapPush Broadcaster
     */
    private Broadcaster getBroadcaster() {
        return bf.lookup(DefaultBroadcaster.class, "MapPush", true);
    }
 
    /**
     * The @PostConstruct annotation makes this method executed by the
     * container after this resource is instanciated. It is one way
     * to initialize the Broadcaster (e.g. by adding some Filters)
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing EventResource");
        BroadcasterConfig config = getBroadcaster().getBroadcasterConfig();
        config.addFilter(new BoundsFilter());
        listener = new EventListener();
        generator = new EventGenerator(getBroadcaster(), 100);
    }
 
    /**
     * When the client connects to this URI, the response is suspended or
     * upgraded if both client and server arc WebSocket capable. A Broadcaster
     * is affected to deliver future messages and manage the
     * communication lifecycle.
     * @param res the AtmosphereResource (injected by the container)
     * @param bounds the bounds (extracted from header and deserialized)
     * @return a SuspendResponse
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SuspendResponse<String> connect(@Context AtmosphereResource res,
            @HeaderParam("X-Map-Bounds") Bounds bounds) {
        if (bounds != null) res.getRequest().setAttribute("bounds", bounds);
        return new SuspendResponse.SuspendResponseBuilder<String>()
                .broadcaster(getBroadcaster())
                .outputComments(true)
                .addListener(listener)
                .build();
    }
 
    /**
     * This URI allows a client to send a new Event that will be broadcaster
     * to all other connected clients.
     * @param event the Event (deserialized from JSON by Jersey)
     * @return a Response
     */
    @POST
    @Path("event")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response broadcastEvent(Event event) {
        logger.info("New event: {}", event);
        getBroadcaster().broadcast(event);
        return Response.ok().build();
    }
 
    // ...
 
}
