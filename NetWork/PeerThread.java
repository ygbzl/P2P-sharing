package NetWork;


import java.io.IOException;
import java.util.Random;

/**
 * Created by leqi on 2017/4/7.
 */
public class PeerThread implements Runnable {
    ActualMsg receiver;
    //Config config;
    //ManageFile fileManager;
    Config.Peer peer;
    int pid;
    int peerIndex;
    Random random;

    PeerThread(Config.Peer peer, int index){
        receiver = new ActualMsg();
        //this.config = config;
        //this.fileManager = fileManager;
        pid = peer.getPID();
        peerIndex = index;
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void run() {
        //peerProcess.config;
        //peerProcess.fileManager;

        if (!peerProcess.config.getMyFile()){
            //I don't have file
            try {
                while (true) {
                    ActualMsg temp = receiver.readActualMsg(peerProcess.config.getPeers().get(peerIndex).getSocket().getInputStream());
                    switch (temp.getType()) {
                        case CHOKE:
                            //received choke message, set flag chokeMe.
                            peerProcess.config.getPeers().get(peerIndex).setChokeMe(true);
                            break;

                        case UNCHOKE:
                            //received unchoke message, set flag chokeMe, send request message.
                            peerProcess.config.getPeers().get(peerIndex).setChokeMe(false);
                            ActualMsg request = new ActualMsg(ActualMsg.MsgType.REQUEST, peerProcess.config.getPeers().get(peerIndex).getBitField().randomSelectIndex(random));
                            request.sendActualMsg(peerProcess.config.getPeers().get(peerIndex).getSocket().getOutputStream());
                            break;

                        case INTERESTED:
                            //received interest message, set flag interestMe.
                            peerProcess.config.getPeers().get(peerIndex).setInterestMe(true);
                            break;

                        case NOTINTERESTED:
                            //received not interest message, set flag interestMe.
                            peerProcess.config.getPeers().get(peerIndex).setInterestMe(false);
                            break;

                        case HAVE:
                            //received have message, update peer's bitfield,
                            // check my bitfield, set flag interested and send interest message if needed.
                            if(peerProcess.config.getMyBitField().isInterested(temp.getIndex())) {
                                //if I am interested in this piece, set flag, send interest message.
                                peerProcess.config.getPeers().get(peerIndex).setInterested(true);
                                ActualMsg interest = new ActualMsg(ActualMsg.MsgType.INTERESTED);
                                interest.sendActualMsg(peerProcess.config.getPeers().get(peerIndex).getSocket().getOutputStream());
                            } else {
                                peerProcess.config.getPeers().get(peerIndex).setInterested(false);
                                ActualMsg notInterest = new ActualMsg(ActualMsg.MsgType.NOTINTERESTED);
                                notInterest.sendActualMsg(peerProcess.config.getPeers().get(peerIndex).getSocket().getOutputStream());
                            }
                            peerProcess.config.getPeers().get(peerIndex).getBitField().setPiece(temp.getIndex());
                            break;

                        case BITFIELD:
                            //nothing to do here
                            break;

                        case REQUEST:
                            //check if this peer is choked
                            if (!peerProcess.config.getPeers().get(peerIndex).getChoked()){
                                //if not choked, then send the piece which is requested.
                                ActualMsg piece = new ActualMsg(peerProcess.fileManager.readMsg(temp.getIndex()));
                                piece.sendActualMsg(peerProcess.config.getPeers().get(peerIndex).getSocket().getOutputStream());
                            } else {
                                //if choked, just ignore this request.
                                break;
                            }
                            break;

                        case PIECE:
                            // received piece message, write the piece into file,
                            // remove the index of piece from interestlist in my bitfield,
                            // check if I am still interested in this peer.
                            // if interest and not be chocked, send another request.
                            peerProcess.fileManager.writeMsg(new FilePiece(temp));
                            peerProcess.config.getMyBitField().removeInterest(temp.getIndex());
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }
}