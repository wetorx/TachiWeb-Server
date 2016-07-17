package xyz.nulldev.ts.api.http;

import spark.Request;
import spark.Response;
import spark.Route;
import xyz.nulldev.ts.Library;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 17/07/16
 */
public abstract class TachiWebRoute implements Route {

    private Library library;

    public TachiWebRoute(Library library) {
        this.library = library;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.header("Access-Control-Allow-Origin", "*");
        ReentrantLock masterLock = library.getMasterLock().get();
        if(masterLock != null) {
            masterLock.lock();
            try {
                Object toReturn = handleReq(request, response);
                masterLock.unlock();
                return toReturn;
            } catch (Throwable e) {
                masterLock.unlock();
                throw e;
            }
        } else {
            return handleReq(request, response);
        }
    }

    public abstract Object handleReq(Request request, Response response) throws Exception;

    public Library getLibrary() {
        return library;
    }
}