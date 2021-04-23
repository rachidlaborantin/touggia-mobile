package com.siliddor.developper.touggia;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.siliddor.developper.touggia.OperationsDB.HeuresSocietes;
import com.siliddor.developper.touggia.OperationsDB.InfosCompagnies;
import com.siliddor.developper.touggia.OperationsDB.Instanciation;
import com.siliddor.developper.touggia.laborantin.Securisation.SecurisationDonnees;
import com.siliddor.developper.touggia.laborantin.Securisation.SecurisationtComposants;

import org.bson.Document;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.FindIterable;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class PageDesSocietes extends AppCompatActivity {

    private static final int CODE_PERMISSION = 85;
    private View mContentView;
    private MongoCollection<Document> collectionSocietes, collectionsTicketsBus, collectionsTicketsRestau,
                                        collectionsTicketsEvenements, collectionHeures;
    private FindIterable<Document> iterable;
    private int nombreTicketsBus, nombreTicketsRestau, nombreTicketsEven;
    private double prix;
    private String heure;
    private ArrayList<InfoSociete> infoSocieteList;
    private ShimmerFrameLayout shimmerFrameLayout;
    private TextView aucuneConnexionInternet;
    private ConnectivityManager connectivityManager;
    private AdaptateurSocietes adaptateurSocietes;
    private RecyclerView listeSocietes;
    private static final String SOCIETE = "societe", NOM_SOCIETE = "nom_societe", CATEGORIE = "categorie_societe", ADMINISTRATEUR = "administrateur", TICKETS_BUS= "tickets_bus",
            TICKETS_RESTAU = "tickets_restau", TRANSPORT_C = "Transport", RESTAURATION_C = "Restauration", HEURES = "heures",
            LISTE_SOCIETES = "liste_societes", PRIX_TICKET_BUS = "prix_ticket_bus", PRIX_TICKET_RESTAU = "prix_ticket_restau", INFO_ADMIN = "info_admin", EVENEMENTS = "evenements", NOM_PRENOM = "nom_prenom",
            DATE = "date", INFORMATIONS_TICKETS = "informations-tickets", DATE_CREATION_COMPTE = "date_creation_compte", ID_ADMIN = "id_admin", NUMERO = "numero", EMAIL = "email",
            ACHETABLE = "achetable", HEURES_DE_DEPARTS = "heures_de_departs", PROCHAIN_DEPART = "prochain_depart", PLATS = "plats", POSSEDANTS= "possedants",
            ACHETES = "achetes", RESTANTS = "restants", TOTAL_GENERE= "total_genere", TOTAL_VENDU = "total_vendu", ACHATS = "achats", TRANSPORT_RESTAURATION = "transport_restauration",
            TRANSPORT = "transport", RESTAURATION = "restauration", EVENEMENT = "evenement", EN_LIGNE = "en_ligne", LOCALE = "local";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_des_compagnies);

        listeSocietes  = findViewById(R.id.listeSocietes);
        shimmerFrameLayout = findViewById(R.id.shimmerPagesSocietes);
        aucuneConnexionInternet = findViewById(R.id.aucuneConnexion);
        infoSocieteList = new ArrayList<>();
        adaptateurSocietes = new AdaptateurSocietes(infoSocieteList);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        listeSocietes.addItemDecoration(itemDecoration);
        listeSocietes.setLayoutManager(new LinearLayoutManager(this));
        listeSocietes.setAdapter(adaptateurSocietes);

        shimmerFrameLayout.startShimmer();

        Realm.init(this);
        User user = Instanciation.ConnexionDB(this);
        if (user != null){
            MongoDatabase baseDeDonnees = Instanciation.BaseDeDonneesSociete(user);
            collectionSocietes = baseDeDonnees.getCollection(SOCIETE);
            collectionsTicketsBus = baseDeDonnees.getCollection(TICKETS_BUS);
            collectionsTicketsRestau = baseDeDonnees.getCollection(TICKETS_RESTAU);
            collectionHeures = baseDeDonnees.getCollection(HEURES);
        }

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null){
                    if (action.equals("finish_societes")){
                        finish();
                    }
                }

            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("finish_societes"));

        //Permission et requete presence connexion internet
        DemanderPermission();
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
        connectivityManager.requestNetwork(request, VerifierConnexionInternet);

        if (ConnexionInternet()){
            RecuperationDonnees(EN_LIGNE);
        }else {
            RecuperationDonnees(LOCALE);
        }

        FloatingActionButton boutonAide = findViewById(R.id.boutonAideSociete);
        boutonAide.setOnClickListener(v -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            AideSocietes aideSocietes = AideSocietes.newInstance("", "");
            aideSocietes.show(fragmentTransaction, "");
        });


    }

    private void RecuperationDonnees(String type){
        shimmerFrameLayout.startShimmer();


        Callable<ArrayList<InfoSociete>> callable;
        if (type.equals(EN_LIGNE)){
            callable = this::RecupererSocietes;
        }else {
            callable = this::RecuperationLocale;
        }

        Observable.fromCallable(callable)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<ArrayList<InfoSociete>>() {
                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull ArrayList<InfoSociete> infoSocietes) {

                        if (!infoSocietes.isEmpty()){
                            infoSocieteList.addAll(infoSocietes);
                            adaptateurSocietes.notifyItemRangeInserted(0, infoSocietes.size());

                            if (type.equals(EN_LIGNE)){
                                EnregistrerInfosSocietes(infoSocietes);
                            }

                            shimmerFrameLayout.stopShimmer();
                            shimmerFrameLayout.setVisibility(View.GONE);

                        }else {
                            shimmerFrameLayout.stopShimmer();
                            shimmerFrameLayout.setVisibility(View.GONE);
                            String message = (type.equals(EN_LIGNE)) ? getString(R.string.aucuneSociete) : getString(R.string.aucuneSocieteLocale);
                            aucuneConnexionInternet.setText(message);
                            aucuneConnexionInternet.setVisibility(View.VISIBLE);
                            aucuneConnexionInternet.startAnimation(AnimationUtils.loadAnimation(PageDesSocietes.this, android.R.anim.fade_in));
                        }

                        dispose();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                        aucuneConnexionInternet.setText(getString(R.string.erreurRecupInfosDesSocietes));
                        aucuneConnexionInternet.setVisibility(View.VISIBLE);
                        aucuneConnexionInternet.startAnimation(AnimationUtils.loadAnimation(PageDesSocietes.this, android.R.anim.fade_in));
                        dispose();
                    }

                    @Override
                    public void onComplete() {
                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                        dispose();
                    }
                });

    }


    private boolean ConnexionInternet(){
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private final ConnectivityManager.NetworkCallback VerifierConnexionInternet = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(() ->{
                //RecuperationDonnees(EN_LIGNE);
                //adaptateurSocietes.notifyItemRangeInserted(0, infoSocieteList.size());
            });
            super.onAvailable(network);



        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            runOnUiThread(() ->{
                //RecuperationDonnees(LOCALE);
            });


        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            runOnUiThread(() -> {
                listeSocietes.setVisibility(View.INVISIBLE);
                listeSocietes.invalidate();
                RecuperationDonnees(LOCALE);
                adaptateurSocietes.notifyItemRangeInserted(0, infoSocieteList.size());
            });


        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        manager.unregisterNetworkCallback(VerifierConnexionInternet);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private ArrayList<InfoSociete> RecupererSocietes(){

        ArrayList<InfoSociete> infoSocietes = new ArrayList<>();

        RealmResultTask<MongoCursor<Document>> resultatRequete = collectionSocietes.find().iterator();
        MongoCursor<Document> cursor = resultatRequete.get();


        while (cursor.hasNext()){
            Document document = cursor.next();

            if (document != null){

                try {
                    String[] composantsSociete = SecurisationtComposants.RecupererComposants(this, SOCIETE);
                    IvParameterSpec vecteurSociete = SecurisationtComposants.ObtenirIV(this, SOCIETE);
                    SecretKey cleSociete = SecurisationDonnees.GenererCle(composantsSociete[1], composantsSociete[0]);

                    String nomSociete = SecurisationDonnees.Dechiffrement(document.getString(NOM_SOCIETE), cleSociete, vecteurSociete);
                    String categorie = SecurisationDonnees.Dechiffrement(document.getString(CATEGORIE), cleSociete, vecteurSociete);
                    String administrateur = document.getString(ID_ADMIN);

                    //String nomSociete = document.getString(NOM_SOCIETE);
                    //String categorie = document.getString(CATEGORIE);
                    //String administrateur = document.getString(ID_ADMIN);
                    switch (categorie){
                        case TRANSPORT:
                            int ticketsBus = Integer.parseInt(RecupererTicketsBus(administrateur));
                            double prixBus = Double.parseDouble(RecupererPrix(administrateur, TRANSPORT));
                            int achetableBus= (VerifierAchatPossible(nomSociete, administrateur, ticketsBus)) ? 1 : 0;
                            infoSocietes.add(new InfoSociete(nomSociete, TRANSPORT, administrateur,
                                    ticketsBus, prixBus, achetableBus));
                            RecupererHeures(administrateur, nomSociete);
                            break;
                        case RESTAURATION:
                            int ticketsRestau = Integer.parseInt(RecupererTicketsRestau(administrateur));
                            double prixRestau = Double.parseDouble(RecupererPrix(administrateur, RESTAURATION));
                            int achetableRestau = (VerifierAchatPossible(nomSociete, administrateur, ticketsRestau)) ? 1 : 0;
                            infoSocietes.add(new InfoSociete(nomSociete, RESTAURATION, administrateur,
                                    ticketsRestau, prixRestau, achetableRestau));
                            break;
                        case TRANSPORT_RESTAURATION:
                            String bus = nomSociete.concat(" - Bus");
                            String restau = nomSociete.concat(" - Restau");

                            int ticketsBus1 = Integer.parseInt(RecupererTicketsBus(administrateur));
                            double prixBus1 = Double.parseDouble(RecupererPrix(administrateur, TRANSPORT));
                            int achetableBus1 = (VerifierAchatPossible(bus, administrateur, ticketsBus1)) ? 1 : 0;

                            int ticketsRestau1 = Integer.parseInt(RecupererTicketsRestau(administrateur));
                            double prixRestau1 = Double.parseDouble(RecupererPrix(administrateur, RESTAURATION));
                            int achetableRestau1 = (VerifierAchatPossible(restau, administrateur, ticketsRestau1)) ? 1 : 0;

                            InfoSociete societeBus = new InfoSociete(bus, TRANSPORT, administrateur, ticketsBus1, prixBus1, achetableBus1);
                            InfoSociete societeRestau = new InfoSociete(restau, RESTAURATION, administrateur, ticketsRestau1, prixRestau1, achetableRestau1);

                            RecupererHeures(administrateur, bus);

                            if (!infoSocietes.contains(societeBus)){
                                infoSocietes.add(societeBus);
                            }

                            if (!infoSocietes.contains(societeRestau)){
                                infoSocietes.add(societeRestau);
                            }
                            break;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }

        }

        return infoSocietes;
    }


    private String RecupererTicketsBus(String administrateur) throws IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException,
            InvalidAlgorithmParameterException {

        Document document = collectionsTicketsBus.findOne(new Document(ID_ADMIN, administrateur)).get();
        if (document != null){
            String[] composantsTicketsBus = SecurisationtComposants.RecupererComposants(this, TICKETS_BUS);
            IvParameterSpec vecteurTicketsBus = new IvParameterSpec(composantsTicketsBus[2].getBytes());
            SecretKey cleTicketsBus = SecurisationDonnees.GenererCle(composantsTicketsBus[1], composantsTicketsBus[0]);
            return SecurisationDonnees.Dechiffrement(document.getString(RESTANTS), cleTicketsBus, vecteurTicketsBus);
            //return document.getString(RESTANTS);
        }else{
            return "0";
        }
    }


    private boolean VerifierAchatPossible(String societe, String administrateur, int ticketsRestants){
        int achetable;
        try {
            AmazonS3 s3Client = Instanciation.RecupererClientS3(this);
            s3Client.getObject(INFORMATIONS_TICKETS, "Tggia." + societe + "*" + administrateur);
            achetable = 1;
        }catch (AmazonClientException e){
            achetable = 0;
        }

        return achetable == 1 && ticketsRestants > 0;
    }



    private String RecupererTicketsRestau(String administrateur) throws IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException {

        Document document = collectionsTicketsRestau.findOne(new Document(ID_ADMIN, administrateur)).get();
        if (document != null){
            String[] composantsTicketsRestau = SecurisationtComposants.RecupererComposants(this, TICKETS_RESTAU);
            IvParameterSpec vecteurTicketsRestau = new IvParameterSpec(composantsTicketsRestau[2].getBytes());
            SecretKey cleTIcketsRestau = SecurisationDonnees.GenererCle(composantsTicketsRestau[1], composantsTicketsRestau[0]);
            return SecurisationDonnees.Dechiffrement(document.getString(RESTANTS), cleTIcketsRestau, vecteurTicketsRestau);
            //return document.getString(RESTANTS);
        }else {
            return "0";
        }
    }


    private int RecupererTicketsEvenements(String administrateur){

        Document document = collectionsTicketsEvenements.findOne(new Document(ID_ADMIN, administrateur)).get();
        if (document != null){
            nombreTicketsEven = document.getInteger(RESTANTS);
        }

        return nombreTicketsEven;
    }

    private String RecupererPrix(String administrateur, String categorie) throws IllegalBlockSizeException,
            NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException, InvalidAlgorithmParameterException {

        if (categorie.equals(TRANSPORT)) {
            Document document = collectionsTicketsBus.findOne(new Document(ID_ADMIN, administrateur)).get();
            if (document != null){
                String[] composantsTicketsBus = SecurisationtComposants.RecupererComposants(this, TICKETS_BUS);
                IvParameterSpec vecteurTicketsBus = new IvParameterSpec(composantsTicketsBus[2].getBytes());
                SecretKey cleTicketsBus = SecurisationDonnees.GenererCle(composantsTicketsBus[1], composantsTicketsBus[0]);
                return SecurisationDonnees.Dechiffrement(document.getString(PRIX_TICKET_BUS), cleTicketsBus, vecteurTicketsBus);
                 //document.getString(PRIX_TICKET_BUS);
            }else{
                return "0";
            }
        }else{
            Document document = collectionsTicketsRestau.findOne(new Document(ID_ADMIN, administrateur)).get();
            if (document != null){
                String[] composantsTicketsRestau = SecurisationtComposants.RecupererComposants(this, TICKETS_RESTAU);
                IvParameterSpec vecteurTicketsRestau = new IvParameterSpec(composantsTicketsRestau[2].getBytes());
                SecretKey cleTIcketsRestau = SecurisationDonnees.GenererCle(composantsTicketsRestau[1], composantsTicketsRestau[0]);
                return SecurisationDonnees.Dechiffrement(document.getString(PRIX_TICKET_RESTAU), cleTIcketsRestau, vecteurTicketsRestau);
                 //document.getString(PRIX_TICKET_RESTAU);
            }else{
                return "0";
            }
        }
    }


    private void RecupererHeures(String administrateur, String societe){
        RealmList<String> listeHeures = new RealmList<>();
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .build();
        Realm realm = Realm.getInstance(configuration);
        Document document = collectionHeures.findOne(new Document(ID_ADMIN, administrateur)).get();
        if (document != null){
            List<Document> documents = document.getList(HEURES_DE_DEPARTS, Document.class);
            for (Document docs : documents) {
                listeHeures.add(docs.getString(DATE));
            }


            if (!listeHeures.isEmpty()){
                realm.executeTransaction(realm1 -> {
                    HeuresSocietes heuresSocietes = new HeuresSocietes();
                    heuresSocietes.setSociete(societe);
                    heuresSocietes.setHeures(listeHeures);
                    heuresSocietes.setProchainDepart(document.getString(PROCHAIN_DEPART));
                    realm1.copyToRealmOrUpdate(heuresSocietes);
                });
            }
        }




    }


    private void EnregistrerInfosSocietes(ArrayList<InfoSociete> infoSocietes){
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .build();
        Realm realm = Realm.getInstance(configuration);

        for (InfoSociete infos : infoSocietes) {
            realm.executeTransaction(realm1 -> {
                InfosCompagnies infosCompagnies = new InfosCompagnies();
                infosCompagnies.setSociete(infos.nomSociete);
                infosCompagnies.setAdministrateur(infos.administrateur);
                infosCompagnies.setCategorie(infos.categorieSociete);
                infosCompagnies.setPrixTicket(infos.prix);
                infosCompagnies.setTicketsDisponibles(infos.nombreTicketsDisponibles);
                infosCompagnies.setAchetable(infos.achetable);
                realm1.copyToRealmOrUpdate(infosCompagnies);
            });
        }
    }

    private ArrayList<InfoSociete> RecuperationLocale(){
        ArrayList<InfoSociete> infoSocietes = new ArrayList<>();
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .build();
        Realm realm = Realm.getInstance(configuration);
        realm.executeTransaction(realm1 -> {
            RealmResults<InfosCompagnies> infosCompagnies = realm1.where(InfosCompagnies.class).findAll();
            for (InfosCompagnies infos : infosCompagnies) {
                infoSocietes.add(new InfoSociete(infos.getSociete(), infos.getAdministrateur(), infos.getCategorie(),
                        infos.getTicketsDisponibles(), infos.getPrixTicket(), infos.getAchetable()));
            }
        });
        return infoSocietes;
    }


    private void DemanderPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODE_PERMISSION){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }else {
                BoiteDinfo boiteDerreur = BoiteDinfo.newInstance(getString(R.string.permissionMemoireT),
                        getString(R.string.permissionMemoireC));
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                boiteDerreur.show(transaction, null );
            }
        }
    }




    private class AdaptateurSocietes extends RecyclerView.Adapter<AdaptateurSocietes.ViewHolder>{

        private final List<InfoSociete> infoSocietes;

        public AdaptateurSocietes(List<InfoSociete> infoSocietes) {
            this.infoSocietes = infoSocietes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vue = getLayoutInflater().inflate(R.layout.vue_compagnie, parent, false);

            return new ViewHolder(vue);
        }

        @Override
        public void onBindViewHolder(AdaptateurSocietes.ViewHolder holder, int position) {

            InfoSociete infoSociete = infoSocietes.get(position);

            TextView nomSociete = holder.nomSociete;
            nomSociete.setText(infoSociete.nomSociete);

            TextView categorieSociete = holder.categorieSociete;
            String categorie = infoSociete.categorieSociete;
            if (categorie != null){
                switch (categorie){
                    case TRANSPORT:
                        categorieSociete.setText(TRANSPORT_C);
                        break;
                    case RESTAURATION:
                        categorieSociete.setText(RESTAURATION_C);
                        break;
                }
            }

            TextView nombreTicketsDispo = holder.nombreTicketsDispo;

            String nombreTD = String.valueOf(infoSociete.nombreTicketsDisponibles).concat(" tickets disponibles");
            nombreTicketsDispo.setText(nombreTD);

        }


        @Override
        public int getItemCount() {
            return infoSocieteList.size();
        }


        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

            public TextView nomSociete, categorieSociete, nombreTicketsDispo, nombreTicketsDispo2;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                nomSociete = itemView.findViewById(R.id.nomSociete);
                categorieSociete = itemView.findViewById(R.id.categorieSociete);
                nombreTicketsDispo = itemView.findViewById(R.id.nombreTicketDispo1);
                nombreTicketsDispo2  = itemView.findViewById(R.id.nombreTicketDispo2);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                Animation animation = AnimationUtils.loadAnimation(PageDesSocietes.this, R.anim.zoom_bouton);
                v.startAnimation(animation);

                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION){
                    InfoSociete infoSociete = infoSocietes.get(position);

                    Intent intent = new Intent(PageDesSocietes.this, PageDacceuil.class);
                    intent.putExtra(SOCIETE, infoSociete.nomSociete);
                    intent.putExtra(CATEGORIE, infoSociete.categorieSociete);
                    intent.putExtra(ADMINISTRATEUR, infoSociete.administrateur);
                    intent.putParcelableArrayListExtra(LISTE_SOCIETES, infoSocieteList);
                    intent.putExtra(ACHETABLE, infoSociete.achetable);

                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sendBroadcast(new Intent("finish"));
        if (isTaskRoot()){
            finishAffinity();
            System.exit(0);
        }
    }
}