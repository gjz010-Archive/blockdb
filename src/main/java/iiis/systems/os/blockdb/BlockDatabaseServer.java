package iiis.systems.os.blockdb;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class BlockDatabaseServer {
    private Server server;

    private void start(String address, int port) throws IOException {
        server = NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                .addService(new BlockDatabaseImpl())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                BlockDatabaseServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        AtomicFileHelper.selfCheck();
        JSONObject config = Util.readJsonFile("config.json");
        config = (JSONObject)config.get("1");
        String address = config.getString("ip");
        int port = Integer.parseInt(config.getString("port"));
        String dataDir = config.getString("dataDir");
        for(String str : args){
            if(str.equals("--enable-random-crash")){
                Util.CRASH_TEST=true;
            }else
            if(str.startsWith("--random-crash-rate=")){
                
                Util.CRASH_RATE=Double.parseDouble(str.replace("--random-crash-rate=", ""));
                System.err.println("Crash rate = "+Util.CRASH_RATE);
            }else
            if(str.equals("--no-atomic-write")){
                AtomicFileHelper.atomicWriteDisabled=true;
            }else if(str.equals("--clean")){
                
                System.err.println("Force cleaning up.");
                try{
                    Files.delete(new File(dataDir+"/latest.json").toPath());
                }catch(Exception ex){
                    System.err.println("Warning: Cleanup Failed.");
                }
            }else{
                System.err.println("Warning: Unknown argument: "+str);
            }
        }
        DatabaseEngine.setup(dataDir);

        final BlockDatabaseServer server = new BlockDatabaseServer();
        server.start(address, port);
        server.blockUntilShutdown();
    }

    static class BlockDatabaseImpl extends BlockDatabaseGrpc.BlockDatabaseImplBase {
        private final DatabaseEngine dbEngine = DatabaseEngine.getInstance();

        @Override
        public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
            int value = dbEngine.get(request.getUserID());
            GetResponse response = GetResponse.newBuilder().setValue(value).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void put(Request request, StreamObserver<BooleanResponse> responseObserver) {
            boolean success = dbEngine.put(request.getUserID(), request.getValue());
            BooleanResponse response = BooleanResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void withdraw(Request request, StreamObserver<BooleanResponse> responseObserver) {
            boolean success = dbEngine.withdraw(request.getUserID(), request.getValue());
            BooleanResponse response = BooleanResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void deposit(Request request, StreamObserver<BooleanResponse> responseObserver) {
            boolean success = dbEngine.deposit(request.getUserID(), request.getValue());
            BooleanResponse response = BooleanResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void transfer(TransferRequest request, StreamObserver<BooleanResponse> responseObserver) {
            boolean success = dbEngine.transfer(request.getFromID(), request.getToID(), request.getValue());
            BooleanResponse response = BooleanResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void logLength(Null request, StreamObserver<GetResponse> responseObserver) {
            int value = dbEngine.getLogLength();
            GetResponse response = GetResponse.newBuilder().setValue(value).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
