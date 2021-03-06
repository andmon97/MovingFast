package it.uniba.di.gruppo17;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.net.URL;
import java.util.ArrayList;
import it.uniba.di.gruppo17.asynchttp.AsyncPastRentals;

import it.uniba.di.gruppo17.util.Keys;
import it.uniba.di.gruppo17.util.PastRentalsAdapter;
import it.uniba.di.gruppo17.util.Rental;

import static android.content.Context.MODE_PRIVATE;
import static it.uniba.di.gruppo17.util.Keys.USER_ID;

/** @author Pasquale, sgarra
 *  Fragment contenente la cardView che mostrerò il totale del tempo trascorso
 *  sui monopattini, il totale dei noleggila e la recycler view che conterrà i noleggi
 */
public class PastRentalsFragment extends Fragment {

    ArrayList<Rental> rentals = null;
    private Integer user_ID;
    private SharedPreferences preferences;
    private TextView TV_total_duration;
    private TextView TV_num_rentals;
    RecyclerView recyclerView;
    PastRentalsAdapter pastRentalsAdapter;
    View view;
    Animation anim;
    MaterialCardView titleCard;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_past_rentals, container, false);

        titleCard = view.findViewById(R.id.statisticsCard);
        anim = AnimationUtils.loadAnimation(getContext(), R.anim.translate_horizontal);
        anim.setDuration(700);
        titleCard.startAnimation(anim);

        TV_total_duration = (TextView) view.findViewById(R.id.valueDurationTextView);
        TV_num_rentals = (TextView) view.findViewById(R.id.valueTotalRentalTextView);

        viewPastRentals();

        return view;
    }

    private void viewPastRentals(){

        Integer durata_totale = 0;
        int ora_totale = 0;
        int minuti_totale = 0;
        /**
         * Creo url connessione
         **/
        preferences = getActivity().getSharedPreferences("MovingFastPreferences", MODE_PRIVATE);
        user_ID = Integer.parseInt(preferences.getAll().get(USER_ID).toString());

        String str = Keys.SERVER + "view_past_rentals.php?id=" + user_ID;
        URL url = null;

        try {
            url = new URL(str);
            AsyncPastRentals pastRentals = new AsyncPastRentals();
            rentals = new ArrayList<>();
            rentals = pastRentals.execute(url).get(); //lista dei noleggi

            /**creiamo le nostre card view**/
            pastRentalsAdapter = new PastRentalsAdapter(rentals);

            /**Calcolo del totale noleggi **/
            String string = String.valueOf(rentals.size());
            TV_num_rentals.setText(string);

            /**Calcolo durata totale noleggi**/
            for (int i =0; i< rentals.size(); i++) {
                durata_totale += rentals.get(i).getDurata();
            }

            ora_totale = (int) durata_totale / 60;
            minuti_totale = (int) durata_totale % 60;


            /**Nelle shared preferences salviamo la durata totale dei noleggi effettuati utile per lo sconto (in minuti)**/

            TV_total_duration.setText(ora_totale+ "h " + minuti_totale + "m");

        }
        catch(Exception e) {
            e.printStackTrace();
        }

        /**Settiamo la nostra recylerView passandogli l'adapter con le card**/
        recyclerView = (RecyclerView) view.findViewById(R.id.pastRentalsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(pastRentalsAdapter);

    }


}
