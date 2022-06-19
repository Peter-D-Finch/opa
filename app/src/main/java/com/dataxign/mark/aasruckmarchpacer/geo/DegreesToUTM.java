package com.dataxign.mark.aasruckmarchpacer.geo;

/**
 * Created by Mark on 3/18/2016.
 * This class converts degrees latitude and longitude to UTM
 */
public class DegreesToUTM {
    static double a = 6378137;      //equatorial radius	a	6378137
    static double b = 6356752.314;  //polar radius	b	6356752.314
    static double f=0.003352811;    //flattening	f	0.003352811
    static double f_i=1.0/f;        //inverse flattening	1/f	298.2572229
    static double rm=6367435.68;    //Mean radius	rm	6367435.68
    static double k0=0.9996;        //scale factor	k0	0.9996
    static double e=0.081819191;    //eccentricity	e	0.081819191
    static double n=0.00167922;     //3d flattening	n	0.00167922
    static double AA=6367449.146;   //Meridian radius	AA	6367449.146
    static double alpha1,alpha2,alpha3,alpha4,alpha5,alpha6,alpha7,alpha8,alpha9,alpha10;

    public DegreesToUTM(){
        computeAlphas();
    }

    public static void computeAlphas(){
        alpha1=(1d/2d)*n - (2d/3d)*Math.pow(n,2) + (5d/16d)*Math.pow(n,3) + (41d/180d)*Math.pow(n,4) - (127d/288d)*Math.pow(n,5) + (7891d/37800d)*Math.pow(n,6) + (72161d/387072d)*Math.pow(n,7) - (18975107d/50803200d)*Math.pow(n,8) + (60193001d/290304000d)*Math.pow(n,9) + (134592031d/1026432000d)*Math.pow(n,10);
        alpha2=(13d/48d)*Math.pow(n,2) - (3d/5d)*Math.pow(n,3) + (557d/1440d)*Math.pow(n,4) + (281d/630d)*Math.pow(n,5) - (1983433d/1935360d)*Math.pow(n,6) + (13769d/28800d)*Math.pow(n,7) + (148003883d/174182400d)*Math.pow(n,8) - (705286231d/465696000d)*Math.pow(n,9) + (1703267974087d/3218890752000d)*Math.pow(n,10);
        alpha3=(61d/240d)*Math.pow(n,3) - (103d/140d)*Math.pow(n,4) + (15061d/26880d)*Math.pow(n,5) + (167603d/181440d)*Math.pow(n,6) - (67102379d/29030400d)*Math.pow(n,7) + (79682431d/79833600d)*Math.pow(n,8) + (6304945039d/2128896000d)*Math.pow(n,9) -  (6601904925257d/1307674368000d)*Math.pow(n,10);
        alpha4=(49561d/161280d)*Math.pow(n,4) - (179d/168d)*Math.pow(n,5) + (6601661d/7257600d)*Math.pow(n,6) + (97445d/49896d)*Math.pow(n,7) - (40176129013d/7664025600d)*Math.pow(n,8) + (138471097d/66528000d)*Math.pow(n,9) + (48087451385201d/5230697472000d)*Math.pow(n,10);
        alpha5=(34729d/80640d)*Math.pow(n,5) - (3418889d/1995840d)*Math.pow(n,6) + (14644087d/9123840d)*Math.pow(n,7) +   (2605413599d/622702080d)*Math.pow(n,8) - (31015475399d/2583060480d)*Math.pow(n,9) +  (5820486440369d/1307674368000d)*Math.pow(n,10);
        alpha6=(212378941d/319334400d)*Math.pow(n,6) - (30705481d/10378368d)*Math.pow(n,7) + (175214326799d/58118860800d)*Math.pow(n,8) + (870492877d/96096000d)*Math.pow(n,9) - (1328004581729000d/47823519744000d)*Math.pow(n,10);
        alpha7=(1522256789d/1383782400d)*Math.pow(n,7) - (16759934899d/3113510400d)*Math.pow(n,8) + (1315149374443d/221405184000d)*Math.pow(n,9) + (71809987837451d/3629463552000d)*Math.pow(n,10);
        alpha8=(1424729850961d/743921418240d)*Math.pow(n,8) -   (256783708069d/25204608000d)*Math.pow(n,9) + (2468749292989890d/203249958912000d)*Math.pow(n,10);
        alpha9=(21091646195357d/6080126976000d)*Math.pow(n,9) - (67196182138355800d/3379030566912000d)*Math.pow(n,10);
        alpha10=(77911515623232800d/12014330904576000d)*Math.pow(n,10);
        //Log.w("Convert:","Alpha1 "+alpha1+" | alpha 2 "+alpha2+" | alpha 3 "+alpha3+" | alpha 4 "+alpha4+" | alpha 5 "+alpha5+" | alpha 6 "+alpha6+" | alpha 7 "+alpha7+" | alpha 8 "+alpha8+" | alpha 9 "+alpha9+" | alpha 10 "+alpha10+" | end");

    }

    public static void latLon2UTM(LocationPoint loc){
        computeAlphas();
        double lat=loc.lat;
        double lon=loc.lon;
        double meridian=-87;
        if(lon>-72 && lon<-71.0)meridian=-69;
        if(lon>-87.5 && lon<-87.0)meridian=-87;

        //Log.w("Convert", "Lat: "+lat+" | Lon: "+lon);
        //double meridian=-87; //165;
        double falseEasting=500000;

        double lata_I2=Math.abs(lat);
        //Log.w("Convert", "Lata_I2: "+lata_I2);
        double latr_I3=lata_I2*Math.PI/180d;
        //Log.w("Convert", "Latr_I3: "+latr_I3);

        double lona_I4 = Math.abs(lon);
        double dlon_I5=Math.abs(lon-meridian);
        //Log.w("Convert", "dlon: "+dlon_I5);
        double dlonr_I6=dlon_I5*Math.PI/180d;
        //Log.w("Convert", "dlonr: "+dlonr_I6);

        double part1=asinh(Math.tan(latr_I3));
        double part2=e * atanh(e * Math.sin(latr_I3));
        //Log.w("Convert", "part1: "+part1+" | part2: "+part2 + " e " + e);

        double confLat_I7 = Math.atan(Math.sinh(asinh(Math.tan(latr_I3)) - e * atanh(e * Math.sin(latr_I3))));
        //Log.w("Convert", "confLat_I7: "+confLat_I7);

        //=SINH(e*ATANH(e*TAN(I3)/SQRT(1+TAN(I3)^2)))
        double sigma_I8 = Math.sinh(e * atanh(e * Math.tan(latr_I3) / Math.sqrt(1 + Math.pow(Math.tan(latr_I3), 2))));


        //Log.w("Convert", "Sigma: " + sigma_I8);
        double confLat_I9 = Math.atan(Math.tan(latr_I3) * Math.sqrt(1 + sigma_I8 * sigma_I8) - sigma_I8 * Math.sqrt(1 + Math.pow(Math.tan(latr_I3), 2)));
        double tau_I10=Math.tan(latr_I3);
        double tau_prime_I11=Math.tan(confLat_I9);
        double xi_prime_I12=Math.atan(tau_prime_I11 / Math.cos(dlonr_I6));
        double eta_prime_I13=asinh(Math.sin(dlonr_I6) / Math.sqrt(tau_prime_I11 * tau_prime_I11 + (Math.pow(Math.cos(dlonr_I6), 2))));
        double xi_I14=xi_prime_I12 + alpha1 * Math.sin(2 * xi_prime_I12) * Math.cosh(2 * eta_prime_I13) + alpha2 * Math.sin(4 * xi_prime_I12) * Math.cosh(4 * eta_prime_I13) + alpha3 * Math.sin(6 * xi_prime_I12) * Math.cosh(6 * eta_prime_I13) + alpha4 * Math.sin(8 * xi_prime_I12) * Math.cosh(8 * eta_prime_I13) + alpha5 * Math.sin(10 * xi_prime_I12) * Math.cosh(10 * eta_prime_I13) + alpha6 * Math.sin(12 * xi_prime_I12) * Math.cosh(12 * eta_prime_I13) + alpha7 * Math.sin(14 * xi_prime_I12)*Math.cosh(14 * eta_prime_I13);
        double eta_I15=eta_prime_I13+alpha1*Math.cos(2 * xi_prime_I12) * Math.sinh(2 * eta_prime_I13)+alpha2 * Math.cos(4 * xi_prime_I12) * Math.sinh(4 * eta_prime_I13)+alpha3 * Math.cos(6 * xi_prime_I12) * Math.sinh(6 * eta_prime_I13)+alpha4 * Math.cos(8 * xi_prime_I12) * Math.sinh(8 * eta_prime_I13)+alpha5 * Math.cos(10 * xi_prime_I12) * Math.sinh(10 * eta_prime_I13)+alpha6 * Math.cos(12 * xi_prime_I12) * Math.sinh(12 * eta_prime_I13)+alpha6 * Math.cos(14 * xi_prime_I12) * Math.sinh(14 * eta_prime_I13);
        double easting=k0*AA*eta_I15;
        double northing=k0*AA*xi_I14;
        //easting=easting+falseEasting; For east of 0
        easting=falseEasting+(-1.0*easting);



        loc.setUTM(easting,northing);
        loc.setMeridian(meridian);
    }

    private static double asinh(double x)
    {
        double ans=Math.log(x + Math.sqrt(x*x + 1.0));
        //Log.w("ASINH","X: "+x+" | "+ans+" | ");
        return ans;
    }

    private static double acosh(double x)
    {
        return Math.log(x + Math.sqrt(x*x - 1.0));
    }

        private static double atanh(double x)
    {
        double ans=0.5*(Math.log(1.0+x)-Math.log(1.0-x));
        //Log.w("ATANH","X: "+x+" | "+ans+" | ");
        return ans;
    }
}
