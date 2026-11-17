/* Android/Java port of GNU Robbo 0.68 (C/SDL). Game logic ported 1:1 from board.c/levels.c/screen.c.
 * Original (c) 2002-2010 The GNU Robbo Team, GPL v2 or later. This port is GPL as well. */
package org.gnurobbo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Robbo extends Activity {
    G g;

    @Override protected void onCreate(Bundle b) { super.onCreate(b); setContentView(g = new G(this)); }
    @Override protected void onResume() { super.onResume(); g.run = true; g.post(g); }
    @Override protected void onPause() { super.onPause(); g.run = false; g.removeCallbacks(g); }
    @Override public void onBackPressed() { if (g.mode != 0) g.mode = 0; else finish(); }

    static class Cell {
        int type, state, dir, destroyable, blowable, killing, moved, blowed, shooted, rotated, solid, rotable,
            rrot, tn, tn2, idq, dir2, movable, ret, shooting, processed;
    }

    static class G extends View implements Runnable {
        static final int EM = 0, RO = 1, WALL = 2, WRED = 3, SCREW = 4, BULLET = 5, BOX = 6, KEY = 7, BOMB = 8,
            DOOR = 9, QM = 10, BEAR = 11, BIRD = 13, CAPS = 15, LB = 21, GROUND = 24, WGREEN = 25, BEARB = 26,
            BFLY = 28, LL = 30, LD = 32, TELE = 40, TELING = 41, BB = 42, GUN = 50, MAG = 54, BLAST = 58,
            WBLACK = 59, PBOX = 60, BARR = 61, WFAT = 63, WROUND = 64, WBOULD = 65, WSQ = 66, WLAT = 67,
            RAD = 68, STOP = 69, BOMB2 = 70, SW = 71;
        // delays (GNU Robbo "smooth scroll" build: 2x ticks, 50 Hz)
        static final int D_RAD = 6, D_BIRD = 8, D_LB = 4, D_BEAR = 8, D_BFLY = 8, D_BB = 6, D_ROBBO = 8,
            D_TELE = 16, D_LASER = 8, D_CAPS = 20, D_GUN = 16, D_ROT = 40, D_BLAST = 8, D_PBOX = 8, D_BARR = 8,
            D_TELING = 6, D_BT = 8, D_RANIM = 4, D_BLINK = 12, D_RESTART = 48;
        static final int SBUL = 1, SSCREW = 2, SBOMB = 3, SBOX = 4, SDOOR = 5, SGUN = 6, SKEY = 7, SSHOOT = 8,
            SBIRD = 9, STELE = 10, SROBBO = 11, SCAPS = 12, SKILL = 13, SMAG = 14, SEXIT = 15;
        static final String[] SN = {"", "ammo", "screw", "bomb", "box", "door", "gun", "key", "shoot", "bird",
            "teleport", "walk", "end", "kill", "magnet", "capsule"};
        static final int[] DX = {1, 0, -1, 0}, DY = {0, 1, 0, -1};
        static final int[][] IC = new int[72][]; // sprite (col,row) pairs per state, 1-based in icons32.png

        static {
            IC[WALL] = new int[]{3, 1, 4, 1, 6, 3, 8, 2, 10, 2, 9, 6, 10, 6, 11, 1, 11, 2, 1, 9, 2, 9, 3, 9, 4, 9,
                5, 9, 6, 9, 7, 9, 8, 9, 1, 10, 2, 10, 3, 10, 4, 10, 5, 10, 6, 10, 7, 10, 8, 10};
            IC[SCREW] = new int[]{5, 1};
            IC[BULLET] = new int[]{6, 1};
            IC[BOX] = new int[]{7, 1};
            IC[PBOX] = new int[]{7, 7, 9, 2};
            IC[KEY] = new int[]{8, 1};
            IC[BOMB2] = new int[]{8, 7};
            IC[BOMB] = new int[]{9, 1};
            IC[DOOR] = new int[]{10, 1};
            IC[QM] = new int[]{1, 2};
            IC[CAPS] = new int[]{6, 2, 7, 2};
            IC[BEAR] = new int[]{2, 2, 3, 2};
            IC[BIRD] = new int[]{4, 2, 5, 2};
            IC[LB] = new int[]{2, 3, 3, 3, 4, 3, 5, 3};
            IC[GROUND] = new int[]{6, 7};
            IC[BEARB] = new int[]{7, 3, 8, 3};
            IC[BFLY] = new int[]{9, 3, 10, 3};
            IC[LL] = new int[]{1, 4, 2, 4, 1, 3, 6, 8};
            IC[LD] = new int[]{3, 4, 4, 4, 6, 4, 7, 8};
            IC[TELE] = new int[]{1, 5, 2, 5};
            IC[TELING] = new int[]{11, 4, 11, 5, 11, 6, 11, 7, 11, 8};
            IC[BB] = new int[]{1, 8, 2, 8, 3, 8, 4, 8, 5, 8};
            IC[GUN] = new int[]{6, 5, 7, 5, 8, 5, 9, 5, 9, 8, 10, 7, 10, 8, 9, 7};
            IC[MAG] = new int[]{1, 1, 1, 7, 2, 1, 2, 7};
            IC[BLAST] = new int[]{3, 5, 4, 5, 5, 5, 4, 5, 3, 5};
            IC[BARR] = new int[]{10, 4, 10, 5};
            IC[RAD] = new int[]{3, 7, 4, 7};
            IC[STOP] = new int[]{5, 7};
        }

        class P { // update_coords() helper: u() returns true (and does nothing) if it would leave the level
            int x, y;
            P(int x, int y) { this.x = x; this.y = y; }
            void s(int x, int y) { this.x = x; this.y = y; }
            boolean u(int d) {
                int a = x + (d == 0 ? 1 : d == 2 ? -1 : 0), b = y + (d == 1 ? 1 : d == 3 ? -1 : 0);
                if (a < 0 || b < 0 || a >= lw || b >= lh) return true;
                x = a; y = b; return false;
            }
        }

        Robbo ctx; Bitmap img; Typeface font; SoundPool sp; int[] snd = new int[16]; SharedPreferences pref;
        Random R = new Random(); Paint pt = new Paint(); Rect sr = new Rect(), dr = new Rect();
        boolean run; int mode; // 0 menu, 1 game, 2 end
        Cell[][] B = new Cell[32][31];
        int lw, lh, lcol, cyc, rx, ry, rst, rdir, scr, keys, bul, rmoved, rshot, rbdir, rt = -1, blink;
        int vw, vh, vx, vy, vcyc, ts, tb, td;
        boolean alive, exitOpen, rblocked, rtele;
        String author = ""; P dest = new P(0, 0);
        String[] pf, pn; int[] plast; int sp_, sel, reach; // packs
        int dir = -1; boolean fire; long nxt;
        float sc, ox, oy, dcx, dcy, pr, fcx, fr, bty; // portrait layout: game on top (virtual 512x480), controls below

        void lay() {
            int W = getWidth(), H = getHeight();
            float ah = Math.max(H - .71f * W, H * .4f);
            sc = Math.min(W / 512f, ah / 480f); ox = (W - 512 * sc) / 2; oy = (ah - 480 * sc) / 2;
            pr = .22f * W; dcx = .27f * W; dcy = H - .3f * W; fcx = .8f * W; fr = .15f * W; bty = H - .61f * W;
        }

        G(Robbo a) {
            super(a); ctx = a;
            for (int i = 0; i < 32; i++) for (int j = 0; j < 31; j++) B[i][j] = new Cell();
            pref = a.getSharedPreferences("robbo", 0);
            try { img = BitmapFactory.decodeStream(a.getAssets().open("icons32.png")); } catch (Exception e) { }
            try { font = Typeface.createFromAsset(a.getAssets(), "robbo.ttf"); } catch (Exception e) { }
            sp = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
            for (int i = 1; i < 16; i++)
                try { snd[i] = sp.load(a.getAssets().openFd("snd/" + SN[i] + ".ogg"), 1); } catch (Exception e) { }
            try {
                String[] fl = a.getAssets().list("levels"), k = new String[fl.length];
                for (int i = 0; i < fl.length; i++) {
                    String n = fl[i], l = "1", s;
                    BufferedReader r = rd(fl[i]);
                    while ((s = r.readLine()) != null && !s.startsWith("[level]")) {
                        if (s.startsWith("[name]")) n = r.readLine().trim();
                        else if (s.startsWith("[last_level]")) l = r.readLine().trim();
                    }
                    r.close();
                    k[i] = n + "\t" + fl[i] + "\t" + l;
                }
                Arrays.sort(k);
                pf = new String[k.length]; pn = new String[k.length]; plast = new int[k.length];
                for (int i = 0; i < k.length; i++) {
                    String[] q = k[i].split("\t");
                    pn[i] = q[0]; pf[i] = q[1]; plast[i] = num(q[2]);
                }
            } catch (Exception e) { pf = new String[0]; }
            for (int i = 0; i < pf.length; i++) if (pn[i].equals("Original")) sp_ = i;
            pack(0);
        }

        BufferedReader rd(String f) throws Exception {
            return new BufferedReader(new InputStreamReader(ctx.getAssets().open("levels/" + f), "ISO-8859-1"));
        }

        int num(String s) { // sscanf("%i")
            try { return Integer.parseInt(s.trim().split("[^0-9-]")[0]); } catch (Exception e) { return Integer.MIN_VALUE; }
        }

        void pack(int d) {
            if (pf.length == 0) return;
            sp_ = (sp_ + d + pf.length) % pf.length;
            sel = reach = pref.getInt(pf[sp_], 1);
        }

        void snd(int i, boolean loud) { if (snd[i] != 0) sp.play(snd[i], loud ? 1f : .125f, loud ? 1f : .125f, 1, 0, 1f); }

        boolean inV(int x, int y) { return x - vx / 8 <= vw && x - vx / 8 >= 0 && y - vy / 8 <= vh && y - vy / 8 >= 0; }

        int rnd() { return R.nextInt(32768); }

        /* ================= board.c ================= */
        void co(int x, int y, int t) { // create_object
            Cell c = B[x][y];
            c.type = t; c.state = c.dir = c.destroyable = c.blowable = c.killing = c.moved = c.blowed = c.shooted = 0;
            c.rotated = c.solid = c.rotable = c.rrot = c.tn = c.tn2 = c.dir2 = c.movable = c.ret = c.shooting = 0;
            c.processed = cyc;
            switch (t) {
                case RO: rx = x; ry = y; rst = 0; c.type = EM; c.idq = 0; return;
                case BEAR: case BEARB: case BIRD: case BFLY: c.killing = 1;
                case BULLET: case BOMB: case BOMB2: case QM: case GROUND: case BARR: c.destroyable = 1;
                case SCREW: case PBOX: case BOX: case KEY: case DOOR: case LL: case LD: case TELE: case GUN:
                case EM: c.blowable = 1; break;
                case STOP: case RAD: c.blowable = 1; c.destroyable = 0; break;
            }
        }

        void cf(int x, int y) { co(x, y, EM); B[x][y].idq = 0; } // clear_field

        boolean oor(int x, int y) { return x < 0 || y < 0 || x >= lw || y >= lh; }

        void neg(int x, int y) { if (x < lw && y < lh) B[x][y].state = B[x][y].state == 0 ? 1 : 0; }

        boolean cm(int x, int y, int d) { // can_move
            P p = new P(x, y);
            return !(p.u(d) || B[p.x][p.y].type != EM || (rx == p.x && ry == p.y) || B[p.x][p.y].moved > 0);
        }

        void mv(int x, int y, P d) { // move_object
            if (x == d.x && y == d.y) return;
            Cell s = B[x][y], t = B[d.x][d.y];
            t.type = s.type; t.state = s.state; t.dir = s.dir; t.destroyable = s.destroyable; t.blowable = s.blowable;
            t.killing = s.killing; t.moved = s.moved; t.blowed = s.blowed; t.shooted = s.shooted;
            t.rotated = s.rotated; t.solid = s.solid; t.rotable = s.rotable; t.rrot = s.rrot; t.idq = s.idq;
            t.dir2 = s.dir2; t.movable = s.movable; t.shooting = s.shooting; t.processed = s.processed;
            cf(x, y);
        }

        void shoot(int x, int y, int d) { // shoot_object
            Cell c = B[x][y];
            if (c.shooted > 0) return;
            P p = new P(x, y);
            if (p.u(d)) { B[p.x][p.y].shooted = D_LASER; return; }
            if (p.x == rx && p.y == ry) { kill(); return; }
            Cell t = B[p.x][p.y];
            if (t.destroyable == 1) {
                if (t.type != BOMB && t.type != BOMB2) snd(SKILL, inV(x, y));
                t.blowed = 1; t.shooted = D_LASER;
                if (c.solid == 1) c.moved = D_LASER;
                return;
            }
            if (t.type == EM) {
                cf(p.x, p.y); co(p.x, p.y, d == 0 || d == 2 ? LL : LD);
                t.dir = d; t.solid = c.solid; c.shooted = D_LASER; t.moved = D_LASER;
                if (t.solid == 1) t.state = c.state == 3 ? c.state : rnd() % 2 + 2;
            }
        }

        void blow(int x, int y, boolean b2) { // blow_bomb / blow_bomb2
            if (alive) snd(SBOMB, inV(x, y));
            if (!alive) { co(x, y, BB); B[x][y].moved = D_BB; return; }
            int d = -1;
            for (int i = 0; i < 9; i++) {
                int x1 = i % 3 > 1 ? -1 : i % 3, y1 = (i / 3) % 3 > 1 ? -1 : (i / 3) % 3, cx = x + x1, cy = y + y1;
                boolean cross = x1 == 0 || y1 == 0;
                if (b2 && cross) d = x1 != 0 ? -x1 + 1 : -y1 + 2;
                if (rx == cx && ry == cy) kill();
                if (b2 && cross) {
                    shoot(x, y, d);
                    if (!oor(cx, cy)) B[cx][cy].moved = D_BT;
                    if (i < 7) B[x][y].shooted = 0;
                } else if (!oor(cx, cy) && B[cx][cy].blowed == 0 && B[cx][cy].blowable != 0) {
                    Cell c = B[cx][cy];
                    if (c.type == GUN) {
                        c.blowed = 1; c.moved = D_BT;
                        if (c.solid != 0) {
                            P lp = new P(cx, cy);
                            while (!lp.u(c.dir) && (B[lp.x][lp.y].type == LD || B[lp.x][lp.y].type == LL)
                                && B[lp.x][lp.y].solid != 0 && B[lp.x][lp.y].dir == c.dir) {
                                B[lp.x][lp.y].blowed = 1; B[lp.x][lp.y].moved = D_BT;
                            }
                        }
                    } else if (!((c.type == LD || c.type == LL) && c.solid != 0)) {
                        c.blowed = 1; c.moved = D_BT; c.idq = 0;
                    }
                }
            }
        }

        void chk(int x, int y) { // check_object_if_blowed
            Cell c = B[x][y];
            if (c.blowed != 0 && c.type != BB) {
                if (c.type == BOMB) blow(x, y, false);
                if (c.type == BOMB2) blow(x, y, true);
                co(x, y, BB); c.moved = D_BB;
            }
        }

        void openExit() { exitOpen = true; blink = D_BLINK; }

        int randId() { return new int[]{EM, PBOX, SCREW, BULLET, KEY, BOMB, GROUND, BFLY, GUN, QM}[rnd() % 10]; }

        boolean findTele(P c, int a, int b) {
            for (int i = 0; i < lw; i++) for (int j = 0; j < lh; j++)
                if (B[i][j].type == TELE && B[i][j].tn == a && B[i][j].tn2 == b) { c.s(i, j); return true; }
            return false;
        }

        boolean isK() { // is_robbo_killed
            return !rtele && ((rx < lw - 1 && B[rx + 1][ry].killing == 1) || (rx > 0 && B[rx - 1][ry].killing == 1)
                || (ry < lh - 1 && B[rx][ry + 1].killing == 1) || (ry > 0 && B[rx][ry - 1].killing == 1));
        }

        void kill() {
            if (!alive) return;
            alive = false; rblocked = false; snd(SBOMB, true);
            co(rx, ry, BB); B[rx][ry].moved = D_BB;
            for (int x = 0; x < lw; x++) for (int y = 0; y < lh; y++)
                if (B[x][y].type != EM && B[x][y].type != WALL) { B[x][y].moved = D_BB; B[x][y].blowed = 1; }
            rt = D_RESTART;
        }

        void mr(int x, int y) { // move_robbo
            if (rblocked || rmoved > 0) return;
            int xt = rx + x, yt = ry + y;
            if (x == 1 && y == 0) rdir = 0; else if (x == 0 && y == 1) rdir = 2;
            else if (x == -1 && y == 0) rdir = 4; else if (x == 0 && y == -1) rdir = 6;
            if (xt < 0 || xt >= lw || yt < 0 || yt >= lh) return;
            Cell c = B[xt][yt];
            switch (c.type) {
                case STOP: cf(xt, yt); break;
                case RAD: cf(xt, yt); kill(); break;
                case EM: case RO: break;
                case SCREW:
                    if (scr > 0) scr--;
                    if (scr == 0) { snd(SEXIT, true); openExit(); } else snd(SSCREW, true);
                    cf(xt, yt); break;
                case BULLET: snd(SBUL, true); bul += 9; cf(xt, yt); break;
                case KEY: snd(SKEY, true); keys++; cf(xt, yt); break;
                case DOOR:
                    if (keys > 0) { keys--; cf(xt, yt); rmoved = D_ROBBO; snd(SDOOR, true); }
                    return;
                case TELE: {
                    if (c.tn == 0) break;
                    snd(STELE, true);
                    int i = 0, j = c.tn2;
                    P co_ = new P(0, 0);
                    while (i == 0) {
                        j++;
                        if (!findTele(co_, c.tn, j) && j != c.tn2) { if (j > 15) j = -1; continue; }
                        int dt = rdir / 2;
                        for (int k = 0; k < 4; k++) {
                            if (cm(co_.x, co_.y, dt)) {
                                co(rx, ry, TELING); co_.u(dt); xt = co_.x; yt = co_.y; rdir = dt * 2;
                                co(xt, yt, TELING); rmoved = D_TELING * 5; vcyc = rmoved; rtele = true; i = 1;
                                break;
                            }
                            dt ^= ((k + 1) % 2) + 2;
                        }
                        if (j == B[xt][yt].tn2 && i == 0) {
                            co(rx, ry, TELING); rmoved = D_TELING * 5; vcyc = rmoved; rtele = true; return;
                        }
                    }
                    break;
                }
                case CAPS:
                    if (exitOpen) {
                        snd(SCAPS, true);
                        if (sel >= plast[sp_]) { mode = 2; return; }
                        sel++;
                        if (sel > reach) { reach = sel; pref.edit().putInt(pf[sp_], reach).commit(); }
                        if (start()) mode = 0;
                        return;
                    } // else falls through: the original pushes a closed capsule like a box
                case BOX: case PBOX: case BOMB: case BOMB2: case QM: case GUN: {
                    if (c.type == GUN && c.movable == 0) return;
                    int x2 = rx + 2 * x, y2 = ry + 2 * y;
                    if (x2 < 0 || x2 >= lw || y2 < 0 || y2 >= lh) return;
                    if (B[x2][y2].type == EM) {
                        snd(SBOX, true);
                        if (c.type == PBOX) { c.state = 1; c.dir = rdir / 2; }
                        mv(xt, yt, new P(x2, y2));
                        if (B[x2][y2].type != GUN) B[x2][y2].moved = D_PBOX;
                    } else { rmoved = D_ROBBO; return; }
                    break;
                }
                default: return;
            }
            rx = xt; ry = yt;
            if (rmoved == 0) { rmoved = D_ROBBO; if (!rblocked) snd(SROBBO, true); }
        }

        void sr(int x, int y) { // shoot_robbo
            if (rshot > 0 || rblocked || rtele) return;
            int xt = rx + x, yt = ry + y;
            if (x == 1 && y == 0) rdir = 0; else if (x == 0 && y == 1) rdir = 2;
            else if (x == -1 && y == 0) rdir = 4; else if (x == 0 && y == -1) rdir = 6;
            if (bul == 0) return;
            if (xt < 0 || xt >= lw || yt < 0 || yt >= lh) { bul--; rshot = D_LASER; return; }
            Cell c = B[xt][yt];
            if (c.destroyable == 1) {
                c.blowed = 1; snd(SSHOOT, false);
                if (c.type != BOMB) snd(SKILL, true);
                rshot = D_LASER; bul--; return;
            }
            if (c.destroyable == 0) { snd(SSHOOT, true); rshot = D_LASER; bul--; }
            if (c.type == EM) { co(xt, yt, rdir == 0 || rdir == 4 ? LL : LD); c.moved = D_LASER; c.dir = rdir / 2; }
        }

        void upd() { // update_game
            dest = new P(0, 0); ts = tb = td = 0;
            if (isK()) kill();
            if (rshot > 0) rshot--;
            if (rmoved > 0) rmoved--;
            if (rmoved == 0) {
                rtele = false;
                if (rblocked) {
                    if (cm(rx, ry, rbdir)) {
                        snd(SMAG, true);
                        if (rbdir == 0) rx++; else if (rbdir == 2) rx--; else if (rbdir == 1) ry++; else ry--;
                        rmoved = D_ROBBO;
                    } else { kill(); return; }
                }
            } else if (rmoved == D_RANIM) rst ^= 1;
            for (int y = 0; y < lh; y++) for (int x = 0; x < lw; x++) {
                Cell c = B[x][y];
                if (c.processed == cyc) continue;
                c.processed = cyc;
                if (c.type == GUN || c.type == BIRD || c.type == MAG) {
                    if (c.shooted > 0) c.shooted--;
                    if (c.rotated > 0) c.rotated--;
                }
                if (c.moved > 0) c.moved--;
                if (c.moved > 0) continue;
                chk(x, y);
                P p = new P(x, y), t = new P(0, 0);
                switch (c.type) {
                    case BEAR: case BEARB: {
                        p.u(c.type == BEAR ? (c.dir + 3) & 3 : (c.dir + 1) & 3);
                        P sb = new P(p.x, p.y); sb.u((c.dir + 2) & 3);
                        P bh = new P(sb.x, sb.y); bh.u(c.type == BEAR ? (c.dir + 1) & 3 : (c.dir + 3) & 3);
                        boolean ff = B[p.x][p.y].type == EM && B[bh.x][bh.y].type == EM
                            && (B[sb.x][sb.y].type == EM || c.type == B[sb.x][sb.y].type);
                        if (B[p.x][p.y].type == EM && !ff) {
                            c.dir = c.type == BEAR ? (c.dir + 3) & 3 : (c.dir + 1) & 3;
                            c.moved = D_BEAR; neg(x, y); mv(x, y, p);
                        } else {
                            p.s(x, y); p.u(c.dir);
                            if (B[p.x][p.y].type == EM) { c.moved = D_BEAR; neg(x, y); mv(x, y, p); }
                            else {
                                c.dir = c.type == BEAR ? (c.dir + 1) & 3 : (c.dir + 3) & 3;
                                c.moved = D_BEAR; c.processed = cyc; neg(x, y);
                            }
                        }
                        break;
                    }
                    case BARR: {
                        int flag = 0, xt = x;
                        if (c.dir == 0) {
                            while (xt < lw && B[xt][y].type != WALL) xt++;
                            xt--;
                            if (B[xt][y].type == BARR) {
                                ts = B[xt][y].state; tb = B[xt][y].blowed; td = B[xt][y].dir;
                                cf(xt, y); neg(xt, y); flag = 1;
                            }
                            while (xt >= 0 && B[xt][y].type != WALL) {
                                xt--;
                                if (xt >= 0 && B[xt][y].type == BARR) {
                                    chk(xt, y); dest.x = xt + 1; dest.y = y;
                                    if (dest.x == rx && dest.y == ry) { kill(); return; }
                                    mv(xt, y, dest);
                                    B[dest.x][dest.y].moved = D_BARR; B[dest.x][dest.y].processed = cyc;
                                    neg(dest.x, dest.y);
                                }
                            }
                            xt++;
                            if (flag == 1) {
                                if (dest.x == rx && dest.y == ry) { kill(); return; }
                                if (rx == xt && ry == y) { kill(); return; }
                                co(xt, y, BARR);
                                B[xt][y].state = ts; B[xt][y].blowed = tb; B[xt][y].dir = td; B[xt][y].moved = D_BARR;
                                neg(xt, y);
                            }
                        } else if (c.dir == 2) {
                            while (xt >= 0 && B[xt][y].type != WALL) xt--;
                            xt++;
                            if (B[xt][y].type == BARR) {
                                ts = B[xt][y].state; tb = B[xt][y].blowed; td = B[xt][y].dir;
                                cf(xt, y); neg(xt, y); flag = 1;
                            }
                            while (xt < lw && B[xt][y].type != WALL) {
                                xt++;
                                if (xt < lw && B[xt][y].type == BARR) {
                                    chk(xt, y); dest.x = xt - 1; dest.y = y;
                                    if (dest.x == rx && dest.y == ry) { kill(); return; }
                                    mv(xt, y, dest);
                                    B[dest.x][dest.y].moved = D_BARR; B[dest.x][dest.y].processed = cyc;
                                    neg(dest.x, dest.y);
                                }
                            }
                            xt--;
                            if (flag == 1 && tb == 0) {
                                if (dest.x == rx && dest.y == ry) { kill(); return; }
                                if (rx == xt && ry == y) { kill(); return; }
                                co(xt, y, BARR);
                                B[xt][y].state = ts; B[xt][y].moved = D_BARR; B[xt][y].dir = td;
                                neg(xt, y);
                            }
                        }
                        break;
                    }
                    case BIRD:
                        if (cm(p.x, p.y, c.dir)) { p.u(c.dir); mv(x, y, p); }
                        else c.dir = (c.dir + 2) & 3;
                        if (B[p.x][p.y].shooting != 0 && B[p.x][p.y].shooted == 0) {
                            if ((rnd() & 7) == 0) { shoot(p.x, p.y, B[p.x][p.y].dir2); snd(SBIRD, inV(x, y)); }
                            else B[p.x][p.y].shooted = D_LASER;
                        }
                        B[p.x][p.y].moved = D_BIRD; neg(p.x, p.y);
                        break;
                    case BFLY: {
                        if (cm(p.x, p.y, c.dir)) { p.u(c.dir); mv(x, y, p); }
                        Cell n = B[p.x][p.y];
                        if ((rnd() & 7) == 0) n.dir = rnd() & 3;
                        else if ((rnd() & 1) == 0) { if (rx > p.x) n.dir = 0; else if (rx < p.x) n.dir = 2; }
                        else { if (ry > p.y) n.dir = 1; else if (ry < p.y) n.dir = 3; }
                        n.moved = D_BFLY; neg(p.x, p.y);
                        break;
                    }
                    case BLAST: {
                        t.s(x, y);
                        int st = B[t.x][t.y].state;
                        while (B[t.x][t.y].type == BLAST) {
                            st = B[t.x][t.y].state;
                            if (t.u((B[t.x][t.y].dir + 2) % 4)) break;
                        }
                        if (st < 3 && B[t.x][t.y].type != GUN) { co(x, y, LB); B[x][y].moved = D_BLAST; break; }
                        if (!p.u(c.dir) && c.state == 0) {
                            if (rx == p.x && ry == p.y) { kill(); return; }
                            Cell n = B[p.x][p.y];
                            switch (n.type) {
                                case DOOR: case WALL: case MAG: case BOX: case TELE: case PBOX: case RAD: case STOP:
                                case GUN: case LD: case LL: case BLAST: case SCREW: case KEY: case CAPS: break;
                                case BOMB: case BOMB2: n.blowed = D_BLAST; break;
                                default:
                                    cf(p.x, p.y); co(p.x, p.y, BLAST);
                                    n.dir = c.dir; n.dir2 = c.dir2; n.moved = D_BLAST;
                            }
                        }
                        if (c.state < 4) { c.state++; c.moved = D_BLAST; } else cf(x, y);
                        break;
                    }
                    case CAPS:
                        if (exitOpen) { neg(x, y); c.moved = D_CAPS; }
                        break;
                    case LB:
                        if (c.state < 3) { c.state++; c.moved = D_LB; } else cf(x, y);
                        break;
                    case LL: case LD:
                        p.u(c.dir);
                        c.moved = D_LASER;
                        if (p.x == rx && p.y == ry && c.ret == 0 && !rtele) { kill(); return; }
                        p.s(x, y);
                        if (c.solid == 0) {
                            neg(x, y);
                            if (cm(p.x, p.y, c.dir)) { p.u(c.dir); mv(x, y, p); B[p.x][p.y].moved = D_LASER; }
                            else if (!p.u(c.dir) && B[p.x][p.y].destroyable != 0) {
                                if (B[p.x][p.y].type != BOMB && B[p.x][p.y].type != BOMB2) snd(SKILL, inV(x, y));
                                cf(x, y); B[p.x][p.y].blowed = 1;
                            } else { cf(x, y); co(x, y, LB); B[x][y].moved = D_LB; break; }
                        } else {
                            t.s(x, y);
                            while (B[t.x][t.y].type == c.type && t.x >= 0 && t.y >= 0 && t.x <= lw && t.y <= lh) {
                                if (t.u((c.dir + 2) & 3)) break;
                            }
                            if (B[t.x][t.y].type != GUN) { co(x, y, LB); break; }
                            t.s(x, y); t.u((c.dir + 2) & 3);
                            if (B[t.x][t.y].type == GUN) {
                                int s2 = c.state == 3 ? 2 : 3;
                                t.s(x, y);
                                while (B[t.x][t.y].type == c.type && B[t.x][t.y].dir == c.dir && t.x >= 0 && t.y >= 0
                                    && t.x <= lw && t.y <= lh) {
                                    B[t.x][t.y].state = s2;
                                    if (t.u(c.dir)) break;
                                }
                            }
                            if (cm(p.x, p.y, c.dir) && c.ret == 0) {
                                p.u(c.dir); shoot(x, y, c.dir); B[p.x][p.y].moved = D_LASER;
                            } else if (c.ret == 1) {
                                p.s(x, y); p.u((c.dir + 2) & 3); cf(x, y);
                                if (B[p.x][p.y].type == GUN) { co(x, y, LB); break; }
                                if (B[p.x][p.y].type == LD || B[p.x][p.y].type == LL) {
                                    B[p.x][p.y].ret = 1; B[p.x][p.y].moved = D_LASER; B[p.x][p.y].processed = cyc;
                                }
                            } else {
                                p.s(x, y);
                                if (!p.u(c.dir)) {
                                    Cell n = B[p.x][p.y];
                                    if ((n.type == LD || n.type == LL) && n.dir == c.dir) break;
                                    if (n.destroyable != 0) {
                                        n.blowed = 1;
                                        if (n.type != BOMB && n.type != BOMB2) snd(SKILL, inV(x, y));
                                    }
                                }
                                c.ret = 1;
                            }
                        }
                        break;
                    case BB:
                        if (c.state < 4) { c.state++; c.moved = D_BB; }
                        else {
                            int i = c.idq;
                            if (i > 0 && alive) {
                                cf(x, y);
                                if (i == QM) c.idq = randId();
                                co(x, y, i);
                                if (i == CAPS) openExit();
                                else if (i == GUN) { c.rotable = 1; c.rrot = 1; c.dir = rnd() & 3; }
                            } else cf(x, y);
                        }
                        break;
                    case RAD: neg(x, y); c.moved = D_RAD; break;
                    case TELE: neg(x, y); c.moved = D_TELE; break;
                    case TELING:
                        if (c.state < 4) { c.state++; c.moved = D_TELING; } else cf(x, y);
                        break;
                    case PBOX:
                        if (c.state == 1) {
                            p.s(x, y);
                            if (!p.u(c.dir)) {
                                if (B[p.x][p.y].type == EM && (p.x != rx || p.y != ry)) mv(x, y, p);
                                else { shoot(x, y, c.dir); c.state = 0; }
                            } else c.state = 0;
                            snd(SBOX, inV(x, y));
                            B[p.x][p.y].moved = D_PBOX;
                        }
                        break;
                    case MAG: {
                        if (rblocked) break;
                        if (c.rotable != 0 && c.rotated == 0) {
                            int d = rnd() & 3; c.dir = d; c.state = d; c.rotated = D_ROT;
                        }
                        boolean fin = false;
                        while (!fin) {
                            if (!p.u(c.dir)) {
                                if (rx == p.x && ry == p.y) { rblocked = true; rbdir = (c.dir + 2) & 3; }
                                else if (B[p.x][p.y].type != EM) fin = true;
                            } else fin = true;
                        }
                        break;
                    }
                    case GUN: {
                        boolean sflag = false;
                        t.s(x, y); t.u(c.dir);
                        if (B[t.x][t.y].dir == c.dir) {
                            int tt = B[t.x][t.y].type;
                            if (tt == BLAST || tt == LL || tt == LD) sflag = true;
                        }
                        if (!sflag) {
                            if (c.movable != 0) {
                                if (cm(p.x, p.y, c.dir2)) {
                                    p.u(c.dir2); mv(x, y, p);
                                    B[p.x][p.y].moved = D_GUN; B[p.x][p.y].state = B[x][y].dir | 4;
                                } else {
                                    c.dir2 = (c.dir2 + 2) & 3; c.state = B[x][y].dir | 4; c.moved = D_GUN;
                                }
                            }
                            Cell g = B[p.x][p.y];
                            if (g.rotable != 0 && g.rotated == 0) {
                                if (g.rrot != 0) {
                                    int d = rnd() & 3;
                                    if (g.movable == 1) g.dir2 = Math.abs(g.dir2 + (d - g.dir)) % 4;
                                    g.dir = d;
                                } else g.dir = (g.dir + 1) & 3;
                                g.rotated = D_ROT;
                            }
                        }
                        Cell g = B[p.x][p.y];
                        if (g.shooted == 0) {
                            if (g.solid == 2) {
                                if ((rnd() & 7) == 0) {
                                    snd(SGUN, inV(x, y));
                                    t.s(p.x, p.y); t.u(g.dir);
                                    if (rx == t.x && ry == t.y && !rtele) { kill(); return; }
                                    Cell n = B[t.x][t.y];
                                    switch (n.type) {
                                        case BLAST: case TELE: case MAG: case WALL: case BOX: case PBOX: case CAPS:
                                        case SCREW: case STOP: case RAD: case KEY: case DOOR: case GUN:
                                            g.shooted = D_LASER; break;
                                        case BOMB2: case BOMB: n.blowed = D_LASER; break;
                                        default:
                                            cf(t.x, t.y); co(t.x, t.y, BLAST);
                                            n.dir = g.dir; n.dir2 = g.dir; n.moved = D_BLAST; g.shooted = D_LASER;
                                    }
                                } else g.shooted = D_LASER;
                            } else if ((rnd() & 7) == 0) { snd(SGUN, inV(x, y)); shoot(p.x, p.y, g.dir); }
                            else g.shooted = D_LASER;
                        }
                        if (g.type == GUN && g.movable == 1) g.state = g.dir + 4; else g.state = g.dir;
                        break;
                    }
                }
            }
        }

        /* ================= levels.c ================= */
        int tc(char c) { // transform_char
            switch (c) {
                case 'a': return SW; case 'X': return STOP; case 'k': return RAD; case '.': return EM;
                case 'R': return RO; case 'O': return WALL; case 'Q': return WRED; case 'T': return SCREW;
                case '\'': return BULLET; case '#': return BOX; case '%': return KEY; case 'B': return BOMB2;
                case 'b': return BOMB; case 'D': return DOOR; case '?': return QM; case '@': return BEAR;
                case '^': return BIRD; case '!': return CAPS; case 'H': return GROUND; case 'o': return WGREEN;
                case '*': return BEARB; case 'V': return BFLY; case '&': return TELE; case '}': return GUN;
                case 'M': return MAG; case '-': return WBLACK; case '~': return PBOX; case '=': return BARR;
                case 'q': return WFAT; case 'p': return WROUND; case 'P': return WBOULD; case 's': return WSQ;
                case 'S': return WLAT; case '+': return STOP; case 'L': return LL; case 'l': return LD;
                case ',': return -1;
                default: return EM;
            }
        }

        boolean isWall(int t) {
            return t == WRED || t == WGREEN || t == WBLACK || t == WFAT || t == WROUND || t == WBOULD || t == WSQ
                || t == WLAT || t == SW;
        }

        void wall(int t, int x, int y) { // create_wall
            co(x, y, WALL);
            B[x][y].state = t == WRED ? 1 : t == WGREEN ? 2 : t == WBLACK ? 3 : t == WFAT ? 4 : t == WROUND ? 5
                : t == WBOULD ? 6 : t == WSQ ? 7 : t == WLAT ? 8 : 9;
        }

        int cw(int x, int y) { // check_wall
            if (B[x][y].type != WALL || B[x][y].state < 9) return -1;
            int r = 0;
            if (y > 0 && B[x][y - 1].type == WALL && B[x][y - 1].state >= 9) r += 1;
            if (x + 1 < lw && B[x + 1][y].type == WALL && B[x + 1][y].state >= 9) r += 2;
            if (y + 1 < lh && B[x][y + 1].type == WALL && B[x][y + 1].state >= 9) r += 4;
            if (x > 0 && B[x - 1][y].type == WALL && B[x - 1][y].state >= 9) r += 8;
            return r;
        }

        ArrayList<String> L; int ix;

        String nx() { return ix + 1 < L.size() && !L.get(ix + 1).startsWith("[") ? L.get(++ix) : ""; }

        int hex(String s) { try { return Integer.parseInt(s.trim(), 16); } catch (Exception e) { return -1; } }

        boolean load(int n) { // load_level_data, true = failed
            try {
                L = new ArrayList<String>();
                BufferedReader r = rd(pf[sp_]); String s;
                while ((s = r.readLine()) != null) L.add(s.replace("\r", ""));
                r.close();
            } catch (Exception e) { return true; }
            int sz = L.size(), rows = 0, scrs = -1; boolean found = false;
            int[] v = new int[16];
            for (ix = 0; ix < sz; ix++) if (L.get(ix).startsWith("[default_level_colour]")) { lcol = hex(nx()); break; }
            for (ix = 0; ix < sz - 1; ix++)
                if (L.get(ix).startsWith("[level]") && num(L.get(ix + 1)) == n) { found = true; ix++; break; }
            if (!found) return true;
            boolean sized = false;
            for (ix++; ix < sz; ix++) {
                String s = L.get(ix);
                if (s.startsWith("[level]") || s.startsWith("[end]")) break;
                if (s.startsWith("[colour]")) lcol = hex(nx());
                else if (s.startsWith("[screws]")) scrs = num(nx());
                else if (s.startsWith("[author]")) author = nx();
                else if (s.startsWith("[size]")) {
                    String[] q = nx().split("\\.");
                    if (q.length > 1) {
                        int w = num(q[0]), h = num(q[1]);
                        if (w > 0 && w <= 32 && h > 0 && h <= 31) { lw = w; lh = h; sized = true; }
                    }
                } else if (s.startsWith("[data]") && sized) {
                    for (int y = 0; y < lh && ix + 1 < sz; y++) {
                        String ln = L.get(++ix); rows++;
                        for (int x = 0; x < lw && x < ln.length(); x++) {
                            int t = tc(ln.charAt(x));
                            co(x, y, t);
                            if (t == SCREW) { if (scrs >= 0) scr = scrs; else scr++; }
                            else if (isWall(t)) wall(t, x, y);
                        }
                    }
                } else if (s.startsWith("[additional]")) {
                    String h = nx(); int cnt = num(h);
                    if (cnt <= 0) continue;
                    for (; cnt > 0 && ix + 1 < sz && !L.get(ix + 1).startsWith("["); cnt--) {
                        String e = L.get(++ix);
                        int d1 = e.indexOf('.'), d2 = d1 < 0 ? -1 : e.indexOf('.', d1 + 1);
                        if (d2 < 0 || d2 + 1 >= e.length()) continue;
                        int x = num(e.substring(0, d1)), y = num(e.substring(d1 + 1, d2));
                        char sym = e.charAt(d2 + 1);
                        String[] q = e.substring(d2 + 2).split("\\.");
                        int vc = 3;
                        for (int k = 0; k < q.length && k < 11; k++) {
                            int z = num(q[k]);
                            if (z == Integer.MIN_VALUE) break;
                            v[k] = z; vc++;
                        }
                        if (vc < 4 || x < 0 || x >= 32 || y < 0 || y >= 31) continue;
                        Cell c = B[x][y];
                        switch (tc(sym)) {
                            case -1:
                                if (isWall(v[0])) wall(v[0], x, y); else { co(x, y, v[0]); c.state = v[1]; }
                                c.dir = v[2]; c.destroyable = v[3]; c.blowable = v[4]; c.killing = v[5];
                                c.movable = v[6]; c.shooted = v[7];
                                break;
                            case LL: case LD: c.dir = v[0]; break;
                            case TELE: c.tn = v[0]; c.tn2 = v[1]; break;
                            case GUN:
                                c.dir = v[0]; c.state = v[0]; c.dir2 = v[1]; c.solid = v[2]; c.movable = v[3];
                                c.rotable = v[4]; c.rrot = v[5];
                                if (c.movable == 1) c.state += 4;
                                break;
                            case MAG: c.state = v[0]; c.rotable = vc > 4 ? v[1] : 0; c.dir = v[0]; break;
                            case BARR: c.dir = v[0]; break;
                            case BIRD: c.dir2 = v[1]; c.shooting = v[2]; c.dir = v[0]; break;
                            case BEARB: case BEAR: c.dir = v[0]; break;
                        }
                    }
                }
            }
            for (int x = 0; x < lw; x++) for (int y = 0; y < lh; y++) { int w = cw(x, y); if (w > 0) B[x][y].state = w + 9; }
            return !(sized && rows >= lh);
        }

        boolean start() { // level_init, true = failed
            alive = true; rst = rdir = scr = keys = bul = rshot = rbdir = 0; rmoved = D_ROBBO;
            exitOpen = rblocked = rtele = false; lw = lh = 0; author = ""; lcol = -1; blink = 0; rt = -1;
            for (int y = 0; y < 31; y++) for (int x = 0; x < 32; x++) cf(x, y);
            if (pf.length == 0 || load(sel)) return true;
            if (scr == 0) openExit();
            for (int i = 0; i < lw; i++) for (int j = 0; j < lh; j++) if (B[i][j].type == QM) B[i][j].idq = randId();
            vw = Math.min(lw, 16); vh = Math.min(lh, 12);
            vx = (rx < lw / 2 ? lw - vw : 0) * 8; vy = (ry < lh / 2 ? lh - vh : 0) * 8;
            vcyc = 80;
            return false;
        }

        void scroll() { // viewport follows Robbo (smooth scroll, 1/8 tile units)
            int px = rx - vw / 2, py = ry - vh / 2;
            px = px < 0 ? 0 : px + vw > lw ? lw - vw : px;
            py = py < 0 ? 0 : py + vh > lh ? lh - vh : py;
            px *= 8; py *= 8;
            if ((vx != px || vy != py) && vcyc > 0) {
                if (vx != px) { int d = Math.max(1, (int) (Math.ceil((float) Math.abs(px - vx) / vcyc) / 8)); vx += vx < px ? d : -d; }
                if (vy != py) { int d = Math.max(1, (int) (Math.ceil((float) Math.abs(py - vy) / vcyc) / 8)); vy += vy < py ? d : -d; }
                vcyc--;
            } else vcyc = 1;
        }

        /* ================= main loop / input ================= */
        void tick() {
            if (mode != 1) return;
            boolean rs = false;
            if (rt > 0) rt--; else if (rt == 0) { rt = -1; rs = true; }
            if (rs) { if (start()) mode = 0; }
            else if (dir >= 0) {
                if (!fire) {
                    if (!rtele && vcyc > 1) vcyc = 1;
                    if (alive) mr(DX[dir], DY[dir]);
                } else if (alive) sr(DX[dir], DY[dir]);
            }
            if (mode == 1) {
                upd(); scroll();
                if (blink > 1) blink--;
            }
            cyc++;
        }

        public void run() {
            if (!run) return;
            long t = SystemClock.uptimeMillis();
            if (nxt < t - 100) nxt = t;
            while (nxt <= t) { tick(); nxt += 20; }
            invalidate();
            postDelayed(this, 10);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int a = e.getActionMasked(), n = e.getPointerCount(), W = getWidth();
            int lift = a == MotionEvent.ACTION_POINTER_UP ? e.getActionIndex() : a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL ? -2 : -1;
            lay();
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
                float x = e.getX(e.getActionIndex()), y = e.getY(e.getActionIndex());
                if (mode == 0 && pf.length > 0) {
                    float fy = (y - oy) / sc - 150;
                    int row = fy < 0 ? -1 : (int) (fy / 80), sg = (x - ox) / sc < 256 ? -1 : 1;
                    if (row == 0) pack(sg);
                    else if (row == 1) sel = Math.max(1, Math.min(reach, sel + sg));
                    else if (row == 2) { mode = 1; if (start()) mode = 0; }
                } else if (mode == 2) mode = 0;
                else if (mode == 1) {
                    if (Math.hypot(x - .12f * W, y - bty) < .09f * W) { rt = -1; if (start()) mode = 0; }
                    else if (Math.hypot(x - .88f * W, y - bty) < .09f * W) mode = 0;
                }
            }
            dir = -1; fire = false;
            for (int i = 0; i < n; i++) {
                if (i == lift || lift == -2) continue;
                float x = e.getX(i), y = e.getY(i), dx = x - dcx, dy = y - dcy, d = (float) Math.hypot(dx, dy);
                if (x < W * .55f) {
                    if (d > pr * .25f && d < pr * 1.6f) dir = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 0 : 2) : (dy > 0 ? 1 : 3);
                } else if (Math.hypot(x - fcx, dy) < fr * 1.4f) fire = true;
            }
            return true;
        }

        /* ================= rendering (virtual 512x480, like the original skin without side margins) ================= */
        void blit(Canvas c, int sx, int sy, int x, int y) {
            sr.set(sx, sy, sx + 32, sy + 32); dr.set(x, y, x + 32, y + 32);
            if (img != null) c.drawBitmap(img, sr, dr, null);
        }

        void txt(Canvas c, String s, float x, float y, float sz, int col, Paint.Align al) {
            pt.setAntiAlias(true); pt.setTypeface(font); pt.setTextSize(sz); pt.setColor(col); pt.setTextAlign(al);
            c.drawText(s, x, y, pt);
        }

        @Override protected void onDraw(Canvas c) {
            int W = getWidth();
            lay();
            c.drawColor(0xff000000);
            c.save(); c.translate(ox, oy); c.scale(sc, sc);
            if (mode == 0) {
                txt(c, "GNU ROBBO", 256, 100, 56, 0xffff8000, Paint.Align.CENTER);
                if (pf.length > 0) {
                    txt(c, "<  " + pn[sp_] + "  >", 256, 210, 36, 0xffffffff, Paint.Align.CENTER);
                    txt(c, "<  Level " + sel + " / " + plast[sp_] + "  >", 256, 290, 36, 0xffffffff, Paint.Align.CENTER);
                    txt(c, "START", 256, 370, 40, 0xff639ace, Paint.Align.CENTER);
                } else txt(c, "no levels found", 256, 240, 30, 0xffff0000, Paint.Align.CENTER);
            } else if (mode == 2) {
                txt(c, "CONGRATULATIONS!", 256, 220, 40, 0xffff8000, Paint.Align.CENTER);
                txt(c, pn[sp_] + " completed", 256, 280, 30, 0xffffffff, Paint.Align.CENTER);
            } else {
                int vxo = (512 - vw * 32) / 2, vyo = (480 - vh * 32) / 2;
                c.save(); c.clipRect(vxo, vyo, vxo + vw * 32, vyo + vh * 32);
                pt.setColor(0xff000000 | (blink > 1 ? 0xffffff : lcol >= 0 ? lcol : 0x608050));
                c.drawRect(vxo, vyo, vxo + vw * 32, vyo + vh * 32, pt);
                for (int ty = vy / 8; ty <= vy / 8 + vh && ty < lh; ty++)
                    for (int tx = vx / 8; tx <= vx / 8 + vw && tx < lw; tx++) {
                        int dx = vxo + (tx * 8 - vx) * 4, dy = vyo + (ty * 8 - vy) * 4;
                        Cell k = B[tx][ty];
                        int[] ic = k.type > 0 && k.type < 72 ? IC[k.type] : null;
                        if (ic != null && k.state >= 0 && k.state * 2 + 1 < ic.length)
                            blit(c, 34 * ic[k.state * 2] - 32, 34 * ic[k.state * 2 + 1] - 32, dx, dy);
                        if (tx == rx && ty == ry && alive && (!rtele || rmoved <= D_TELING))
                            blit(c, 2 + 34 * (rdir + rst), 172, dx, dy);
                    }
                c.restore();
                int sy = 240 + vh * 16 + 8;
                blit(c, 138, 104, 0, sy); blit(c, 206, 104, 72, sy); blit(c, 240, 104, 152, sy); blit(c, 274, 104, 248, sy);
                txt(c, String.format("%02d", scr), 32, sy + 26, 28, 0xffffff00, Paint.Align.LEFT);
                txt(c, String.format("%02d", keys), 112, sy + 26, 28, 0xffffff00, Paint.Align.LEFT);
                txt(c, String.format("%03d", bul), 192, sy + 26, 28, 0xffffff00, Paint.Align.LEFT);
                txt(c, String.format("%03d", sel), 288, sy + 26, 28, 0xffffff00, Paint.Align.LEFT);
                txt(c, "-" + pn[sp_], 336, sy + 26, 28, 0xffffff00, Paint.Align.LEFT);
                if (author.length() > 0)
                    txt(c, author.charAt(0) == '_' ? author.substring(1) : "Author: " + author, 4, 240 - vh * 16 - 16, 22, 0xffffffff, Paint.Align.LEFT);
            }
            c.restore();
            if (mode == 1) { // touch controls below the game: d-pad left, FIRE right, R = restart, M = menu
                pt.setTypeface(Typeface.DEFAULT); pt.setTextAlign(Paint.Align.CENTER);
                pt.setColor(0x55ffffff); pt.setStyle(Paint.Style.STROKE); pt.setStrokeWidth(4);
                c.drawCircle(dcx, dcy, pr, pt); c.drawCircle(fcx, dcy, fr, pt);
                c.drawCircle(.12f * W, bty, .07f * W, pt); c.drawCircle(.88f * W, bty, .07f * W, pt);
                pt.setStyle(Paint.Style.FILL); pt.setTextSize(pr * .5f); pt.setColor(0x88ffffff);
                c.drawText("\u25B2", dcx, dcy - pr * .55f, pt); c.drawText("\u25BC", dcx, dcy + pr * .85f, pt);
                c.drawText("\u25C0", dcx - pr * .65f, dcy + pr * .17f, pt); c.drawText("\u25B6", dcx + pr * .65f, dcy + pr * .17f, pt);
                pt.setTextSize(.06f * W); c.drawText(fire ? "[FIRE]" : "FIRE", fcx, dcy + .02f * W, pt);
                pt.setTextSize(.07f * W); c.drawText("R", .12f * W, bty + .025f * W, pt); c.drawText("M", .88f * W, bty + .025f * W, pt);
            }
        }
    }
}
