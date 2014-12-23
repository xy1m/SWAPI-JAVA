/**
 * Created by the-nightphoenix on 12/23/14.
 */

import com.swapi.models.Film;
import com.swapi.models.SWModelList;
import  com.swapi.sw.StarWars;
import com.swapi.sw.StarWarsApi;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Main {

    public static void main(String[] args)
    {
        StarWarsApi.init();
        StarWars api = StarWarsApi.getApi();

        api.getAllFilms(1, new Callback<SWModelList<Film>>() {
        	
            public void success(SWModelList<Film> filmSWModelList, Response response) {
                System.out.println("Count:"+ filmSWModelList.count);
                for(Film f : filmSWModelList.results) {
                    System.out.println("Title:" + f.title);
                }
            }


            public void failure(RetrofitError error) {

            }
        });


    }
}
