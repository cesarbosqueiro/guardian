package com.craftwar.guardian.agent.session;

interface Sender {

    void send(int type, byte[] payload);
}
