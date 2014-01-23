package play.scalasupport

import akka.actor.ActorSystem

class Actors private () {
    
        
    private[this] var _actorSystem = ActorSystem("play-scala")
    
    def actorSystem() = _actorSystem
    
    
}

object Actors {
        
    private var _instance = new Actors()
    
    def actorSystem() = _instance.actorSystem

    def start() = if(actorSystem.isTerminated){
        _instance = new Actors
    } else {
        //nop
    }
    
    def shutDown() = {
        _instance.actorSystem.shutdown()
        _instance.actorSystem.awaitTermination()
    }
    
    
}