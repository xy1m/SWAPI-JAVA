package com.swapi;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.swapi.models.Film;
import com.swapi.models.FilmViewPojo;
import com.swapi.models.People;
import com.swapi.models.SWModelList;
import com.swapi.sw.StarWars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class ZumeController {
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    static final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));
    static int MAX_WAIT_TIME = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZumeController.class);

    @Resource
    private StarWars starWars;

    @RequestMapping("/")
    public String index() {
        return "Greetings from Zhenpeng!";
    }

    @RequestMapping("/films-of-director/")
    public String filmsByDirector() throws InterruptedException, IOException {
        Multimap<String, FilmViewPojo> map = LinkedListMultimap.create();
        CountDownLatch latch = new CountDownLatch(1);
        buildMap(1, map, latch);
        latch.await();

        Gson gson = new Gson();
        return gson.toJson(map.asMap());
    }

    public void buildMap(int page, Multimap<String, FilmViewPojo> map, CountDownLatch latch) throws IOException {
        starWars.getAllFilms(page, new Callback<SWModelList<Film>>() {
            public void success(SWModelList<Film> filmSWModelList, Response response) {
                for (Film f : filmSWModelList.results) {
                    map.put(f.director, new FilmViewPojo(f.title, f.episodeId));
                }
                if (filmSWModelList.hasMore()) {
                    try {
                        buildMap(page + 1, map, latch);
                    } catch (IOException e) {
                        LOGGER.error("", e);
                        latch.countDown();
                    }
                } else {
                    latch.countDown();
                }
            }

            public void failure(RetrofitError error) {
                latch.countDown();
            }
        });
    }

    @RequestMapping("/characters-by-movie/{movieId}")
    public String charactersByMovie(@PathVariable int movieId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> names = new ArrayList<>();
        // fill names
        starWars.getFilm(movieId, new Callback<Film>() {
            public void success(Film film, Response response) {
                ExecutorService exec = Executors.newCachedThreadPool();
                for (final String url : film.charactersUrls) {
                    exec.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                names.add(parsePeople(restapi(url)));
                            } catch (IOException e) {
                                LOGGER.error("Exception for API:" + url, e);
                            }
                        }
                    });
                }
                exec.shutdown();
                try {
                    exec.awaitTermination(MAX_WAIT_TIME, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.error("Time out", e);
                } finally {
                    latch.countDown();
                }
            }

            public void failure(RetrofitError error) {
                LOGGER.error("", error.getCause());
                latch.countDown();
            }
        });
        latch.await();
        Gson gson = new Gson();
        return gson.toJson(names);
    }


    public HttpResponse restapi(String url) throws IOException {
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url).set("format", "json"));
        return request.execute();
    }

    private String parsePeople(HttpResponse response) throws IOException {
        People people = response.parseAs(People.class);
        return people.name;
    }

    private String parseFilm(HttpResponse response) throws IOException {
        Film film = response.parseAs(Film.class);
        return film.title;
    }
}