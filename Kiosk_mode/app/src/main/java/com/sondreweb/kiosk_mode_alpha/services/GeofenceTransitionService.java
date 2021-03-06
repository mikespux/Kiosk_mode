package com.sondreweb.kiosk_mode_alpha.services;

/**
 * Created by sondre on 03-Mar-17.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import com.sondreweb.kiosk_mode_alpha.R;
import com.sondreweb.kiosk_mode_alpha.activities.LoginAdminActivity;
import com.sondreweb.kiosk_mode_alpha.classes.GeofenceStatus;
import com.sondreweb.kiosk_mode_alpha.storage.SQLiteHelper;
import com.sondreweb.kiosk_mode_alpha.utils.AppUtils;
import com.sondreweb.kiosk_mode_alpha.utils.PreferenceUtils;

import java.util.Date;
import java.util.List;

/**
 * Servicen skal kjøre i bakgrunn.
 *
 * Holder koblingen til GooglePlayServices Apiet med en GoogleApiClient.
 *
 * hente lokasjon via GPS evt. WIFI ved angitte intervaller.
 *
 * Oppsetter Geofencene vi trenger
 *
 * Tar imot all respons fra Geofencene når de Triggeres(sender event tilbake hit).
 * Tegner et Overlay på skjermen basert på respons fra Geofencene.
 *
 * Holde skjermen våken.
 */

/**
*   THREAD HANDELING
*   Activity.runOnUiThread(Runnable)
*   View.post(Runnable)
*   View.postDelayed(Runnable, long)
* */

    /**Steg for å lage et Geofence.
     *  1. Lag GoogleApiClient.
     *  2. GoogleApiClient connect.
     *  3. Lag Geofencene vi trenger.
     *  4. Lag GeofenceRequest.
     *  5. sende dette GeofenceRequest  LocationServices.GeofencingApi.addGeofences(GeofenceRequest)
     *  6.Start monitorering av GeofenceRequestet.
     * **/

public class GeofenceTransitionService extends Service implements
        LocationListener, FusedLocationProviderApi, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>{

    private static final String STOP_KIOSK = "com.sondreweb.STOP_KIOSK";


    public static final String TEST_OVERLAY = "com.test.overlay";


    private static boolean geofence_running = false;
    GoogleApiClient googleApiClient;

        //Testing:
        private Button overLayButton;

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤Broadcast reviever TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/
    //Tags that we are listening for go here, to keep track of everyone this service is listening for.
    private static final String INTENT_FILTER_TAG = "com.sondreweb.GeoFencingAlpha.Activity.RestartGeofencing";

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤END TAGS¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤*/

    public static final String OUTSIDE_GEOFENCE_TRIGGERED = "com.monumentvandring.launcher.outside.geofence";
    /*
    *   Når mobilen restartes av en eller ann grunn og viss den forsatt skal være i Kiosk mode, må vi reRegistrere Geofencne våre. Siden de forsvinner fra minne.
    * */
    public static final String START_GEOFENCE = "com.geofence.start";
    public static final String STOP_GEOFENCE_MONITORING = "com.geofence.stop.monitoring";
    public static final String RESTART_GEOFENCES = "com.geofence.restart";

    public static final String START_SERVICE = "com.geofence.service.start";

    public static final String TRIGGERED_GEOFENCE = "com.geofence.triggered";

    private static final String TAG = GeofenceTransitionService.class.getSimpleName();

    private final int mId = 2222; //tilfeldig ID for Notifikasjonen.

    private NotificationCompat.Builder notificationBuilder;

    private NotificationManager notificationMananger;

    private final Handler handler = new Handler();//

    private int count = 0;

    public GeofenceTransitionService(Context applicationContext){//construktor to make an object of the class to reference too in MainActivity.
        super();
    }

    public GeofenceTransitionService(){} //default constructor

    private ServiceBroadcastReceiver serviceBroadcastReceiver;

    private PowerManager.WakeLock wakeLock;
    private boolean isLocationPollingEnabled = true;

    private WindowManager windowManager;



    //Holder oversikt over alle statusene på Geofencene. Om vi er innefor ett eller flere geofence sammtidig.
    private List<GeofenceStatus> geofenceStatusList = null;

        @NonNull
        private String getGeofenceStatus() {
            if(geofence_running){
                return getResources().getString(R.string.service_geofence_on);
            }
            return getResources().getString(R.string.service_geofence_off);
        }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");

        notificationMananger = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this); //Instansier NotificatioBuilder.
        startInForeground(); //setter opp notifikasjonen for å kjøre i forgrunn(høy prioritet på ressursbruk av systemet).

        //Intent filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);//Tar imot når skjermen skrus av
        filter.addAction(STOP_KIOSK);   //Når vi skrur av Kiosk mode via notifikasjonsbar.
        //BroadcastRecieveren vår
        serviceBroadcastReceiver = new ServiceBroadcastReceiver();

        // Registrer Recievereren for bruk, med filter vi lagde over.
        registerReceiver(serviceBroadcastReceiver, filter);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);;
    }

    public Context getContext(){
        return getApplicationContext();
    }


    //Pending intents will start This again(i think).
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //Log.d(TAG,"onStartCommand: intentAction: "+ intent.getAction()+" ##################################################");

        // we can also check wheter the action is from the GeofenceClass or simply starting up the service again.
         //dersom vi starter servicen med hensikt å starte lokasjons håndtering

        GeofencingEvent geofencingEvent = null;
        //Switcher på hvilke Action vi har fått inn på intent.
        switch (intent.getAction()) {

            case START_GEOFENCE: //når vi trykker på knappen for å starte opp locationRequests og slike ting.
                Log.d(TAG, "Start LocationRequests fra servicen  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
                if (createGoogleApi()) {
                    if (!googleApiClient.isConnected()) {
                        googleApiClient.connect(); //Connecter til googleApiClient.
                    } else { //Dersom man allerede er connectet så må man sjekke om vi må refreshe GeofenceListen vår.
                    }
                }
                break;
            case RESTART_GEOFENCES:
                //Dersom enheten er restertet under vandring.
                if(PreferenceUtils.isKioskModeActivated(getApplicationContext())){
                    if(createGoogleApi()){
                        if(!googleApiClient.isConnected()){
                            googleApiClient.connect();
                        }
                        //GoogleApiClient er connected. så alt skal forhåpentligvis være oppe og gå.
                        //TODO gjør det vi må. her.
                    }
                }

                break;
            case TRIGGERED_GEOFENCE: //betyr at vi er kommet hit pga et eller flere Geofence er triggered.
                if(AppUtils.DEBUG) {
                    Log.d(TAG, "Triggered Geofence motatt ++++++++++++++");
                }

                geofencingEvent = GeofencingEvent.fromIntent(intent); //henter Geofencet fra intent viss intent kommer fra et GeofenceClass.
                if (geofencingEvent.hasError()) {
                    /*
                    *   Er 3 forskjellige Error meldinger:
                    *           GEOFENCE_NOT_AVAILABLE : Geofence service is not available now.
                    *           GEOFENCE_TOO_MANY_GEOFENCES : Your app has registered more than 100 geofences.
                    *           GEOFENCE_TOO_MANY_PENDING_INTENTS : You have provided more than 5 different PendingIntents to
                    *                               the addGeofences(GoogleApiClient, GeofencingRequest, PendingIntent) call.
                    * */
                    if( ! AppUtils.checkLocationAvailabillity(getContext(),getGoogleApiClient())){
                        //Betyr at vi ikke har Location eller GoogleAPiClient ikke har Location tilgjengelig.
                        Log.e(TAG,"Vi har ikke location tilgjengelig");
                    }
                } else {
                    //sender event videre.
                    updateGeofenceStatus(geofencingEvent.getGeofenceTransition(), geofencingEvent.getTriggeringGeofences());
                }
                break;
            case START_SERVICE:
                //når vi starte servicen, men er ingen ekstre ting som må gjøres her foreløpig.
                break;
            case STOP_GEOFENCE_MONITORING:
                //toggleView();
                //tellUserToGoInsideButtonToggle();
                //tellUserToGoInsideGeofence();
                stopGeofenceMonitoring();
                startInForeground();
                break;
            case TEST_OVERLAY:
                //Kunn til utvikling hensikt.
                Log.d(TAG,"ToogleOverLay Testing :::::::::::::::::::::::::::::::::::::::");
                //toggleView();
                break;
            default:
                //Dette vill si at vi servicen starter opp av seg selv, eller at vi simpelten starter den opp i bakgrunn.
                break;
        }
        //Error handeling
        //startGeofencing();
        return START_STICKY; //When service is killed by the system, it will start up again.
    }


        /**
         *  StartGeofenceMonitoring:
         *  lager nødvendige Lister.
         *  Henter Alle Geofence fra databasen og starter opp det med å monitorere osv.
         */

    public void startGeofenceMonitoring(){
        //ArrayList som holder på alle Geofence med deres status, som et GeofenceStatus object.
        geofenceStatusList = new ArrayList<>();

        //Henter listen over alle Geofence som er lagret i Database.
        List<Geofence> geofenceList = SQLiteHelper.getInstance(getContext()).getAllGeofences();
        SQLiteHelper.getInstance(getContext()).getAllGeofences();

        //Går igjennom hvert geofence.
        for (Geofence geofence:
             geofenceList) {
            //For hvert geofence, må vi lage status av denne og legge til en oversikts liste.
            geofenceStatusList.add(new GeofenceStatus(geofence));
        }

        //sender med alle Geofence som vi skal monitorere for å lage et GeofenceRequest av disse.
        GeofencingRequest geofenceRequest = createGeofenceRequest(geofenceList);

        //Starter locationUpdates, gjør slik at enheten polle etter GPS koordinatene sine i valgt intervall,
        // trengs for å oppdatere posisjonen regelmessig mee Geofencet.
        startLocationUpdates(getContext());

        //Sender dette GeofenceReuestet videre til LocationServices.GeofencingApi.addGeofences.
        // Ber om at disse geofenen skal monitoreres. Når vi da oppdatere Posisjonen vår, blir disse sjekket.
        addGeofencesToMonitor(geofenceRequest);

        //Setter en boolean på at geofence kjørere.
        setGeofence_running(true);
    }

    /*
    *  Oppdatere Geofencen sin status, utifra hvilken som ble triggered med hvilken transtion id(ENTER eller EXIT)
    *   Geofence har alle Request Id lik geofence_+(Tall de registret i listen)
    * */
    private void updateGeofenceStatus(int geofenceTransition,List<Geofence> triggeredeGeofences){

        if(AppUtils.DEBUG){
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()));
        }

        //For hvert Geofence som triggere må vi finne tilsvarende GeofenceStatus og oppdatere denne.
        for (Geofence geofence : triggeredeGeofences)
        {   //Finner tilsvarende geofence i status listen og oppdaterer.
            for(GeofenceStatus geofenceStatus : geofenceStatusList){
                if(geofence.getRequestId().equalsIgnoreCase(geofenceStatus.getGeofence().getRequestId())){
                    //siden de er like, så må vi oppdatere denne
                    geofenceStatus.setStatus(geofenceTransition);
                }
            }
        }

        if(AppUtils.DEBUG){//Debug info.
            String transition = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ? "Enter" : "Exit";
            Log.d(TAG,"Etter oppdatering: " + transition);
            for(GeofenceStatus geofenceStatus :geofenceStatusList){
                Log.d(TAG,"Geofence navn:"+ geofenceStatus.getGeofence().getRequestId() +", innefor status:"+geofenceStatus.getInsideStatus());
            }
        }

            //Sjekker at vi er minst innenfor et Geofence.
        if( ! checkIfInsideAtleastOneGeofence()){
            //Kommer vi hit vill det si at brukeren er utenfor alle Geofencene som er satt opp.

            if(AppUtils.DEBUG){
                Log.d(TAG,"VIKTIG: Vi er utenfor geofencene!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                Handler mHandler = new Handler();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Vi er utenfor et geofence!!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            //Må sjekke om vi allerede har startet med rask oppdatering av location updates.
            if(fastLocationUpdates){//Dersom denne er true, vill det si at vi allerede drev med å hente rask lokasjon.

                //Vi skal da heller ikke oppdatere eller gjore noe.
                //Og siden vi forsatt viser Viewet, så skal vi ikke gjøre noe mer der heller.
            }else{
                setLocationUpdateChange(true); //Setter fastLocationUpdate to true, og starter rasker oppdatering.

                if(PreferenceUtils.getVibrateSettings(getContext())){
                    vibratePhone(500);
                    //kan virbrere telefonen her, men er bedre om aktivitetn i vindu gjør dette for oss.
                }

                if(PreferenceUtils.getPrefOverlayOn(getApplicationContext())){//Kjapp sjekk på om vi faktisk har på Overlay.
                    //Legger Viewet over på skjermen, som blokerer brukergrensnittet.
                    showViewOnScreen();
                }
            }
            //Brukeren er innfor minst ett Geofence.
        }else{
            //må sjekke om vi driver med normal oppdatering av lokasjon
            if(!fastLocationUpdates){ //dersom denne er false, vill det si at vi er innefor geofencene, og at vi er på riktig intervalltid.
                if(AppUtils.DEBUG){
                    Log.d(TAG,"fastLocationIpdets == true");
                }
                //Trenger ikke gjøre noe her.
            }else{ //Betyr at brukeren nettop gikk tilbake innenfor geofencene.
                //Går tilbake til trengere intervall.
                if(AppUtils.DEBUG){
                    Log.d(TAG,"fastLocationIpdets == false");
                }
                setLocationUpdateChange(false);
                try {
                    if(AppUtils.DEBUG){
                        Log.d(TAG, "Prøver å fjerne overlay");
                    }
                    if(overLayTextview != null) { //Dersom den er initialisert, vill det si at vi har brukt den.
                        //Fjerner overlayViewet fra winduet, siden vi er innefor Geofencene igjen.
                        windowManager.removeView(overLayTextview);
                        if(AppUtils.DEBUG){
                            Log.d(TAG, "overLayTextView != null true");
                        }
                    }
                }catch (IllegalArgumentException e){
                    //Feilhåndtering, dersom det er noe med overLayTextview som tilsier at vi ikke kan remove det.
                    Log.e(TAG, e.getMessage());
                }
            }
            //startLocationUpdates(getContext());
        }
    }

    //TextViewet som vi legger over skjermen, ved behov.
    private TextView overLayTextview;

        //vis et overlayView på skjermen som dimmer bakgrunn og viser noe tekst.
    private void showViewOnScreen(){
        overLayTextview = new TextView(this);
        //Lager teksten som skal være inni tekstboksen over.
        String textToShow = "Overstepping boundary!\n"
                + getResources().getString(R.string.service_geofence_outside_view_text_eng)+"\n"
                + "Oversteget grensene!\n"
                + getResources().getString(R.string.service_geofence_outside_view_text_nor)
                + "\n"+getResources().getString(R.string.service_geofence_outside_view_update_text)
                +" "+ PreferenceUtils.getOutsideGeofenceUpdateIntervalAsString(getApplicationContext());
        //Setter teksten på tekstboksen.
        overLayTextview.setText(textToShow);
        //Legger til en bakgrunns farge på tekstboksen som er litt gjennomskinnelig(translucent).
        overLayTextview.setBackgroundResource(R.color.light_yellow3_transparent);
        //Forandrer tekst fargen.
        overLayTextview.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        //Gir testfeltet en større boks en selve teksten.40dp left, 10dp top, 40dp right og 30dp bottom.
        overLayTextview.setPadding(40,10,40,30);
        //Setter posisjon på tekten i tekstbokse, sentrert i dette tilfelle horisontalt.
        overLayTextview.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //Forandrer tekst størrelse
        overLayTextview.setTextSize(getResources().getDimension(R.dimen.text_mediumSmall));
        //lager parameterene som gjelder for TextViewet.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                //Hva slags type View det er vi legger over, SYSTEM_OVERLAY er det vi har.
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                //Setter at skjermen skal Dimme det som ikke er tekstboksen når det blir vist.
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                //gjør at systemet velger et format som gir oss tilgang til å forandre på hvor mye transulsent hele viewet skal være.
                PixelFormat.TRANSLUCENT);
        //Posisjonerer tekstboksen i sentrum.
        params.gravity = Gravity.CENTER;
        //Hvor mye skjermen skal dimme, når tekstboksen vises.
        params.dimAmount = 0.5f;
        //Legger dette til window.
        windowManager.addView(overLayTextview,params);
    }


    //Sjekker listen på om vi er innefor minst et geofence.
    public boolean checkIfInsideAtleastOneGeofence(){
        //vi må sjekke om minst et er true, så kan vi returne true, ellers er vi utenfor alle.
        for (GeofenceStatus geofenceStatus: geofenceStatusList) {
            //geofenceStatus returnere true dersom siste trigger er ENTER på geofencet.
            if(geofenceStatus.getInsideStatus()){
                //geofenceStatus.getInsideStatus  returner en bool på om
                if(AppUtils.DEBUG){
                    Log.d(TAG, "checkIfInsidegeofence:" + true);
                }
                return true;
            }
        }
        if(AppUtils.DEBUG){
            Log.d(TAG, "checkIfInsidegeofence:" + false);
        }
        return false;
    }

    //For når brukere bevegers seg utenfor Geofence og vi trenger å få deres oppmerksomhet.
    public void vibratePhone(int timeInMillis){
        //vibre i angitte time i ms.
        Vibrator vibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(timeInMillis); //Hvor lenge skal vi vibrere?

    }

    /*
    *   Geofence funksjoner
    * */
        //lager et geofence, og returnere det. Men ble heller gjordt direkte i SQLitehelperen.
    private Geofence createGeofence(LatLng latLng, float radius){
        return new Geofence.Builder()
                .setCircularRegion(latLng.latitude, latLng.longitude,radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    /*
    *   GoogleApiClient connection
    * */

    private boolean createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            Log.d(TAG,"googleApiClient: "+googleApiClient.toString());
        }
        return true;
    }

    /* GET FUNCTIONS */
    public GoogleApiClient getGoogleApiClient(){
        if(googleApiClient != null){
            return googleApiClient;
        }
        this.createGoogleApi();
        return googleApiClient;
    }

    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GeofenceRequest START
    *
    *   GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    *   Vi setter GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT , som betyr:
    *   Alle vi er innenfor triggere Enter, mens alle vi er utenfor triggere EXIT. VI får da 2 EventLister tilbake med geofence tilbake.
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */
    //create GeofenceRequests

    @NonNull
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence) //.addGeofence : Adds a geofence to be monitored by geofencing service.
                .build();
    }

    //GeofenceBuilder.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
    /*
    *   Legger til flere Geofence samtidig i en  og samme Request.
    * */
    @NonNull
    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {
        Log.d(TAG, "createGeofenceRequest()");
        return new GeofencingRequest.Builder()
                //.setInitialTrigger: Sets the geofence notification behavior at the moment when the geofences are added.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
                //.addGeofences: Adds all the geofences in the given list to be monitored by geofencing service.
                .addGeofences(geofences)
                //.build: Builds the GeofencingRequest object.
                .build();
    }

    /*
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *   GeofenceRequest END
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    /*
    *   Legg til GeofenceRequest til device monitoring list.
    *
    * */

    private void addGeofencesToMonitor(GeofencingRequest geofenceRequest) {
        if(AppUtils.DEBUG){
            Log.d(TAG, "addGeofence()");
        }
        if (AppUtils.checkLocationPermission(getContext())) {//om vi har rettigheter til å få tilgang til Lokasjon.
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient, //GoogleApiClienten servicen har koblet seg til.
                    geofenceRequest, //GeofenceRequestet vi lagde med alle geofencene våre.
                    getGeofencePendingIntent() //Henter PendingIntent, eller lager dersom det er tomt.
            ).setResultCallback(this); //ResultCallback -> OnResult.
        } else{
            if(AppUtils.DEBUG){
                Log.d(TAG, "addGeofence: Manglet permission");
            }
        }
    }

    /*
    *   Stop monitorering av Geofencene, siden vi ikke skal ha enheten i bruk lenger.
    *   Skal aktivers når vi tryker på å skru av Kiosk mode.
    * */

    public void stopGeofenceMonitoring(){
        if(googleApiClient != null) {
            setGeofence_running(false); //Setter denne til false;
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            if (googleApiClient.isConnected()) {
                getGoogleApiClient().disconnect();
            }
        }
    }

     /*
     * In case network location provider is disabled by the user, the geofence service will stop updating, all registered geofences
     * will be removed and an intent is generated by the provided pending intent.
     * In this case, the GeofencingEvent created from this intent represents an error event,
     * here hasError() returns true and getErrorCode() returns GEOFENCE_NOT_AVAILABLE.
     * */
    private int GEOFENCE_REQ_CODE = 0;

    private PendingIntent geoFencePendingIntent;

            /* Intent for servicen som skal håndtere Geofence eventene */
    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "getGeofencePendingIntent()");
        //gjenbruker Pendingintentet vårt dersom der alt er laget.
        if (geoFencePendingIntent != null) {
            return geoFencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionService.class); //The intent wich will be called when events trigger.
        intent.setAction(TRIGGERED_GEOFENCE); //Legger til en string, slik at vi kan finne ut om det inneholder et Geofence event eller ikke.
        //FLAG_UPDATE_CURRENT slik at vi får samme Pending intent tilbake når vi kaller addGeofence eller Remove Geofence.
        geoFencePendingIntent = PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoFencePendingIntent;

        //PendingIntent.getService returnerer PendingIntent som skal starte servicen
        /*Retrieve a PendingIntent that will start a service, like calling Context.startService(). The start arguments given to the service will come from the extras of the Intent.
        * */
    }

    /* Notification build up:
    * | Icon | Title
    *         SubText
    * */

    private void startInForeground(){ //vi må kalle denn viss vi skal oppdatere notifikasjonen.
        Log.d(TAG,"startInForeground() / Update notification");
        //TODO: use input to change the notification.

        //TODO: Figure out what pending intent does here.

        notificationBuilder
                .setSmallIcon(getNotificationIcon()) //icon that the user see in status bar.
                .setContentTitle(getResources().getString(R.string.service_title)) // GeofenceClass Service
                .setContentText(getResources().getString(R.string.service_text)) // Text; Location being monitored.
                .setSubText(getResources().getString(R.string.service_status_geofence) + " " + getGeofenceStatus()) //lan lot centrum geofence
                .setTicker("Service starting") //geofence running
                .setPriority(NotificationCompat.PRIORITY_MAX);//Makes the system prioritize this notification over the others(or the same as other with max

        //For å starte opp Loggin activity.

        Intent loggInIntent = new Intent(this,LoginAdminActivity.class);
        loggInIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingInlogging = PendingIntent.getActivity(this,0,loggInIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        //tømmer alle actionene på notifikasjonsbare, siden vi adder flere senere.
        notificationBuilder.mActions.clear();

        //Intent for å sette KioskMode til OFF/kunn for testing.
        if(AppUtils.DEBUG){
            Intent stopKiosk = new Intent(STOP_KIOSK);
            PendingIntent pendingStopKiosk = PendingIntent.getBroadcast(getContext(),0,stopKiosk,PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(R.drawable.unlock_50, "Stop Kioks Mode",pendingStopKiosk);
        }

        //Lager Knappen på notifikasjonen, som gir oss tilgang til å logge inn.
        notificationBuilder.addAction(R.drawable.login_48, "Log In", pendingInlogging );

        startForeground(mId, notificationBuilder.build()); //Start showing the notification on the (Action)/task bar.
    }

    private int getNotificationIcon(){//Versjons kontroll på hvilket bilde å bruke.
        //Dersom vi trenger å gjøre iconet mer synelig på forskjelige versjoner kan dette utdypes, ellers kan man legge til egne iconer for hver versjon i Drawables.
        //boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        return R.drawable.visible_50;

    }

    private void setGeofence_running(boolean running){
        geofence_running = running;
        startInForeground();//oppdatere notifikasjonen vår.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        Intent broadcastIntent = new Intent(INTENT_FILTER_TAG);
        stopForeground(true);
        //handler.removeCallbacks(getGpsCoordinate); //fjerner tråden.
        //handler.notify(); //bare tester hva denne gjør.
        //sendBroadcast(broadcastIntent);
        //stopTimerTask();
    }

    //todo: asynctask for getting the locations.

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String getErrorString(int errorCode)
    {
        switch (errorCode)
        {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeofenceClass not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many Geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Uknown error";
        }
    }
/*
*   BroadcastReciever
*   Mottar Broadcast om ACTION_SCREEN_OFF.
*   Vekker enheten derskom dette mottas, slik at skjermen ikke skrus av under bruk.
* */
    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"------------------------------------------------------------");
            Log.d(TAG,"VI mottok et intent:"+intent.getAction());
            switch (intent.getAction()){
                case Intent.ACTION_SCREEN_OFF:
                    Log.d(TAG, "*****************************************************");
                    Log.d(TAG, "Action Screen Off recieved");
                /*
                Intent i = new Intent(context, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                */
                    if(PreferenceUtils.isKioskModeActivated(context)){ //vi vekker bare skjermen dersom dette.
                        getFullWakeLock().acquire();
                        getFullWakeLock().release();
                    }
                    break;
                case GeofenceTransitionService.STOP_KIOSK:
                    Log.d(TAG, "Vi setter Kioks mode til false");
                    Log.d(TAG,"Kiosk mode nå: "+PreferenceUtils.isKioskModeActivated(context));
                    PreferenceUtils.setKioskModeActive(context,false);
                    Log.d(TAG,"Kiosk mode nå etter forandring: "+PreferenceUtils.isKioskModeActivated(context));
                    break;
            }
        }
    }

    //###############################LOCATION UPDATE STARTING############################


    private LocationRequest locationRequest;
    private LocationRequest fastLocationRequest;

    private boolean fastLocationUpdates = false;

    /*
    *   setPriority: PRIORITY_BALANCED_POWER_ACCURACY : request "block" level accuracy.
    *                PRIORITY_HIGH_ACCURACY : request the most accurate locations available.
    *                PRIORITY_LOW_POWER : request "city" level accuracy.
                     PRIORITY_NO_POWER : request the best accuracy possible with zero additional power consumption.
    * */

    //starter location updates.
    public void startLocationUpdates(Context context) {//TODO: Bytt til GPS kordinater, fremfor Wifi etc.
        /*
        * setIntervall vill si tiden mellom hver etterspørsel av lokasjon. Ivertfall da vi prøver å hente lokasjon.
        *   dersom dette ikke skulle gå så får vi ikke noen lokasjon til neste intervalls tid.
        * setFasterIntervall forteller hvor ofte vi tillater oss å lese av nyeste lokasjon og oppdatere vår egen,
        *     dersom andre applikasjoner osv henter lokasjon sammtidig, kan vi også få inn resultatet.
        * */
        if(AppUtils.DEBUG){
            Log.d(TAG,"startLocationUpdates()");
        }
        locationRequest = locationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) //request the most accurate locations available. (tror dette alltid er GPS).
                .setInterval(PreferenceUtils.getGeofenceUpdateInterval(context))
                .setFastestInterval(PreferenceUtils.DEFAULT_GEOFENCE_FASTEST_UPDATE_INTERVAL);

        if (AppUtils.checkLocationPermission(context)) {
            //starter å hente lokasjonen med requesten.
            Log.d(TAG, "startLocationUpdate() checkLocationPermission: true");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            isLocationPollingEnabled = true;
            //this refererer til callbacks, som er oss selv.
        }
    }

    /*
    *   Bytte Update intervall på lokasjon
    * */
    public void setLocationUpdateChange(boolean fastUpdate){
        /*
        *   Denne må fungere som en flanke trigger. Når vi bytter fra false til true, og fra false til true, ikke ellers.
        * */
        //Vi drev med normal lokasjons intervall, men må bytte til raskere intervall
        if(! fastLocationUpdates && fastUpdate){ // Vi driver med normal lokasjons intervall
            if(AppUtils.DEBUG){
                Log.d(TAG,"Vi driver med normal lokasjons intervall");
            }
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this); //fjerne oppdatering fra cliente.
            startFastLocationUpdates();
        }else if(fastLocationUpdates && !fastUpdate){ //Vi drev med raskt intervall, men må bytte til tregere intervall
            if(AppUtils.DEBUG){
                Log.d(TAG,"Vi driver med raskt intrervall, men bytter til normal hastighet");
            }
            //Siden vi bytter locationUpdate metode, må vi stoppe oppdatering på lokasjon.
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this); //fjerne oppdatering fra cliente.
            startLocationUpdates(getContext());
        }else{ //vi skal ikke gjøre noe her. Siden vi drev alerede med normal oppdatering og skal heller ikke bytte til fastUpdate.
            if(AppUtils.DEBUG){
                Log.d(TAG,"Vi skal ikke gjøre noe");
            }
        }
        fastLocationUpdates = fastUpdate; //oppdatere verdien.
    }


    //Bytter hastighet vi henter ut lokasjonen via GPS.
    private void startFastLocationUpdates(){
        fastLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(PreferenceUtils.getOutsideGeofenceUpdateInterval(getApplicationContext()))
                .setFastestInterval(PreferenceUtils.ONE_SECOND_IN_MILLIS);

        if (AppUtils.checkLocationPermission(getContext())) {
                //starter å hente lokasjonen med requesten.
            if(AppUtils.DEBUG) {
                Log.d(TAG, "startFastLocationUpdate()");
            }
            //Starter opp denne fastLocationRequest via location APIet.
            LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(),fastLocationRequest,this);

            isLocationPollingEnabled = true;
        }
    }
    //###############################LOCATION UPDATE END############################

    /*
    *   Wake screen kode. Ikke sikkert denne koden fungere på nyere APIer enn 19. Fra de må dette håndteres på en annen måte.
    * */
    PowerManager.WakeLock fullWakeLock; //Holder på Wakelocken. Viss vi trenger den til senere.

    public PowerManager.WakeLock getFullWakeLock(){

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if(fullWakeLock == null){
                return fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "FULL WAKE LOCK");
            }
        return fullWakeLock;
    }
    /*
    *   End Keep screen awake code.
    * */

    /*  ResultCallback
    *   Ved addGeofencesToMonitor får vi tilbake melding her på hvordan dette gikk
    * */
    @Override
    public void onResult(@NonNull Status status) {
        if(status.isSuccess()){
            //Toast.makeText(this,"Geofences created successfully", Toast.LENGTH_SHORT).show();
        }else if(status.hasResolution()){
           //TODO: feilmeldinger på å lage Geofence.
            Log.d(TAG,"statuscode: "+status.getStatusCode()+"\n statusMessage: "+status.getStatusMessage()+" \nstatusResolution: "+status.getResolution());
        }
        else
        {
            Log.e(TAG, "Registering failed: " + status.getStatusMessage()+ " code:"+status.getStatusCode());
        }
    }

    /**¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     *  LocationListener
     *  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
     */

    @Override
    public void onLocationChanged(Location location) { //lytter til når location forandres, på denne måten kan vi lytte etter locationChange flere steder.
        if(AppUtils.DEBUG) {
            Log.d(TAG, "LocationChanged location: " + location.getLatitude() +", "+ location.getLongitude());
        }
        Log.d(TAG, "LocationChanged location: " + location.getLatitude() +", "+ location.getLongitude());
    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    /*   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.ConnectionCallbacks
    *    ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override  //Mister connection med GoogleApiClient
    public void onConnectionSuspended(int cause) {
        if(PreferenceUtils.isKioskModeActivated(getContext())){
            //Dersom vi forsatt skal egentlig være connected, så er dette et problem.
            if(AppUtils.DEBUG){
                Log.d(TAG,"onConnectionSuspended");
            }

            if(PreferenceUtils.isKioskModeActivated(getContext())){
                //Dette kan våre et problem.
            }

        }

        /*
        * Called when the client is temporarily in a disconnected state.
        * This can happen if there is a problem with the remote service (e.g. a crash or resource problem causes it to be killed by the system).
         * When called, all requests have been canceled and no outstanding listeners will be executed. GoogleApiClient will automatically attempt to restore the connection.
        * Applications should disable UI components that require the service, and wait for a call to onConnected(Bundle) to re-enable them.
        * */
    }

    /*
    *   Når Vi connecter til Google API Client.
    *   Veldig viktig at vi her starter med alt vi må.
    * */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Dette betyr at vi kan lage geofencene våre og starte med alt som har med det å gjøre.
        if(AppUtils.DEBUG){
            Log.d(TAG, "onConnected()+++++++++++++++++++++++++++++++++++++++++++++++");
        }
        startGeofenceMonitoring();
    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    /*  ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *           GoogleApiClient.OnConnectionFailedListener
    *   ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO: be activity som motar meldingen starte
        //connectionResult.startResolutionForResult();
    }
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *         FusedLocationProviderApi
    * ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

    @Override
    public Location getLastLocation(GoogleApiClient googleApiClient) {

        return null;
    }

    @Override
    public LocationAvailability getLocationAvailability(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener, Looper looper) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
        return null;
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, PendingIntent pendingIntent) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationListener locationListener) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
        return null;
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationCallback locationCallback) {
        return null;
    }

    @Override
    public PendingResult<Status> setMockMode(GoogleApiClient googleApiClient, boolean b) {
        return null;
    }

    @Override
    public PendingResult<Status> setMockLocation(GoogleApiClient googleApiClient, Location location) {
        return null;
    }

    @Override
    public PendingResult<Status> flushLocations(GoogleApiClient googleApiClient) {
        return null;
    }

    /*¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    *         FusedLocationProviderApi END
    * ¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤
    * */

}
