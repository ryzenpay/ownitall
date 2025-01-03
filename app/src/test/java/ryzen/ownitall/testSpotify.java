package ryzen.ownitall;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

class testSpotify { // TODO: write tests fuck face

    private Spotify spotify;

    @BeforeEach
    void setUp() {
        String client_id = "8cf63653de3c45bf9155a0cb39e06c8a";
        String client_secret = "";
        String redirect_url = "https://ryzen.rip/ownitall";
        spotify = new Spotify(client_id, client_secret, redirect_url);
    }
}
