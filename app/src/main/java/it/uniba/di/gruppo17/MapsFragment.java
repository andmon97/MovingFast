package it.uniba.di.gruppo17;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import it.uniba.di.gruppo17.asynchttp.AsyncGetScooters;
import it.uniba.di.gruppo17.util.Scooter;
import it.uniba.di.gruppo17.services.LocationService;

import static it.uniba.di.gruppo17.util.Keys.MAP_ANIMATION_DURATION;
import static it.uniba.di.gruppo17.util.Keys.ZOOM;
import static it.uniba.di.gruppo17.util.Keys.SERVER;
import static it.uniba.di.gruppo17.util.Keys.RAGGIO;

/**
 * @author Francesco Moramarco
 */
public class MapsFragment extends Fragment {

    private static final String TAG ="MapsFragment";
    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.maps_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
                mGoogleMap.setMyLocationEnabled(true);

           if ( getDeviceLocation() )
           {
               getScooters(LocationService.realTimeDeviceLocation());
               mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                   @Override
                   public void onMapLoaded() {
                       setScootersMarker();
                   }
               });

               //Bottom Sheet
               mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                   @Override
                   public boolean onMarkerClick(Marker marker) {
                       marker.hideInfoWindow();
                       final BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogTheme);
                       mBottomSheetDialog.setContentView(R.layout.bottomsheet_layout_mapfragment);
                       TextView address = mBottomSheetDialog.findViewById(R.id.address_textView);
                       TextView battery = mBottomSheetDialog.findViewById(R.id.battery_textView);

                       //Prendo i dati dal marker
                       final LatLng mLatLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
                       Geocoder mGeocoder = new Geocoder(getContext(), Locale.ITALY);
                       try {
                           List<Address> addresses = mGeocoder.getFromLocation(mLatLng.latitude, mLatLng.longitude, 1);
                           address.setText(addresses.get(0).getAddressLine(0));
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       battery.setText(marker.getSnippet().substring(15));

                       mBottomSheetDialog.findViewById(R.id.directionsButton).setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {
                               mBottomSheetDialog.dismiss();
                               //Per aprire app google maps
                               Uri googleMapsIntentUri = Uri.parse("google.navigation:q="+mLatLng.latitude+","+mLatLng.longitude+"&mode=w");
                               Intent googleMapsIntent = new Intent(Intent.ACTION_VIEW, googleMapsIntentUri);
                               googleMapsIntent.setPackage("com.google.android.apps.maps");
                               startActivity(googleMapsIntent);
                               //Metto il fragment in pausa. Da controllare
                               MapsFragment.super.onPause();
                           }
                       });
                       mBottomSheetDialog.findViewById(R.id.unlockButton).setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {
                               mBottomSheetDialog.dismiss();
                               //Per sbloccare il monopattino, richiamo rentFragment ed elimino mapsfragment
                               Fragment toRentFragment = new RentFragment();
                               getFragmentManager().beginTransaction().replace(R.id.maps_fragment, toRentFragment).commit();
                               MapsFragment.super.onDestroy();
                           }
                       });
                       mBottomSheetDialog.show();
                       return true;
                   }
               });
           }
        }
    };

    /**
     * Metodo per ottenere la poszione corrente del dispositivo
     * @author Francesco Moramarco
     */
    private boolean getDeviceLocation()
    {
        if ( LocationService.realTimeDeviceLocation() != null )
          {
              cameraAnimation( LocationService.realTimeDeviceLocation() );
              Snackbar.make(getView(),R.string.device_position_retrieved_message, Snackbar.LENGTH_LONG).show();
          }
        else
          {
              Snackbar.make(getView(),R.string.device_position_NOT_retrieved_message,Snackbar.LENGTH_LONG).show();
              return false;
          }
          return true;
    }

    /**
     * Metodo per modificare l'animazione della telecamera della mappa
     * @param currentLocation
     */
    private void cameraAnimation(final Location currentLocation)
    {
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, ZOOM);
        mGoogleMap.animateCamera(cameraUpdate, MAP_ANIMATION_DURATION, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                //Terminata l'animazione sulla posizione corrente, eseguo la richiesta di scooter al server
                Snackbar.make(getView(),R.string.loading_scooter_message, Snackbar.LENGTH_SHORT).show();
                getScooters(currentLocation);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    /**
     * @author Francesco Moramarco
     * Metodo per ottenere l'elenco dei monopottino dispoonibili dal server
     */
    private void getScooters(Location currentLocation)
    {
        String serverAddress = SERVER + "get_monopattini.php?r="+RAGGIO+"&lat="+currentLocation.getLatitude()+"&long="+currentLocation.getLongitude();
        AsyncGetScooters getScooters = null;
        try{
            URL urlScooters = new URL (serverAddress);
            getScooters = new AsyncGetScooters();
            getScooters.execute(urlScooters);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author Francesco Moramarco
     * Metodo per visualizzare i monopattini sulla mappa mediante marker
     */
    private void setScootersMarker()
    {
        if ( Scooter.nearScooters == null )
        {
            Snackbar.make(getView(),R.string.no_scooter_message,Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Provo a ricaricare
                            getFragmentManager().beginTransaction().detach(MapsFragment.this).attach(MapsFragment.this).commit();
                        }
                    })
                    .show();
            return;
        }
        if ( Scooter.nearScooters.isEmpty() )
        {
            Snackbar.make(getView(),R.string.no_scooter_message,Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Provo a ricaricare
                            getFragmentManager().beginTransaction().detach(MapsFragment.this).attach(MapsFragment.this).commit();
                        }
                    })
                    .show();
            return;
        }
         for(Scooter m : Scooter.nearScooters )
        {
            LatLng coordinate = new LatLng( Double.parseDouble( m.getLatitude() ), Double.parseDouble( m.getLongitude() ) );
            MarkerOptions mMarkerOptions = new MarkerOptions()
                    .position( coordinate )
                    .title(getResources().getString(R.string.title_marker_map)+" #"+m.getIdScooter())
                    .snippet(getResources().getString(R.string.snippet_marker_map)+": "+m.getBatteryLevel()+"%")
                    .visible(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) );
            mGoogleMap.addMarker( mMarkerOptions );
        }
         Snackbar.make(getView(),R.string.completed,Snackbar.LENGTH_SHORT).show();
    }

}
