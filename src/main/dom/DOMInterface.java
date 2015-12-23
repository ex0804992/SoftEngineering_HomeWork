package main.dom;

import com.google.gson.Gson;
import main.sprite.*;
import main.sprite.Character;

import java.util.ArrayList;

/**
 * Created by dblab on 2015/12/23.
 */
public interface DOMInterface {

    public void initMyCharacter(int ID,double x, double y);    //call by TCP.

    public Character getMyCharacter();   //call by Scene.

    public ArrayList<Character> getOtherCharacter();   //call by Scene.

    public Character updateMyPosition();   //call by UDP. For uploading data to server.

    public void downloadCharacter(String gson);   //call by UDP. For getting data from server.


}
