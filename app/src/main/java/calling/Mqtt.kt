package calling

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttClient.generateClientId


//client의 첫번째 인자 context룰 위해 매개변수로 전달 받음
class Mqtt(val context: Context, val uri:String = "tcp://15.165.174.55:1883") {
    //"tcp://172.20.10.6:1883"
    //"tcp://15.165.174.55:1883"

    /*broker에 접속하기 위한 메소드*/

    //1. client
    //uri : 브로커에 대한 uri(경로) , MqttClient.genearteClientId() : mqtt가 제공하는 메소드로 아이디 설정
    var mqttClient:MqttAndroidClient = MqttAndroidClient(context, uri ,generateClientId())

    //2.mqtt클라이언트가 connection을 해야한다
    fun connect(topics:Array<String>?=null){
        //연결에 관련된 여러가지 설정 정보를 담고 있는 Option객체 같이 넘겨줘야한다
        // => 위에서 만들어놓은 mqttclient의 connect메소드를 호출할 때 인자로 들어간다
        val mqttConnectOption = MqttConnectOptions() //여러가지 옵션도 설정가능하지만 일단 그냥한다
        Log.d("mymqtt","mqtt서버에 들어옴")
        //connect의 인자(여러가지 설정정보 담은 option, userContext;우리는 쓰지 않는다, callback)
        //callback함수로 IMqttToken의 하위(객체 리스너)를 만들어서 지정하면 연결이 실패했는지 성공했는지 알 수 있음
        mqttClient.connect(mqttConnectOption,null,object:IMqttActionListener{

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("mymqtt","mqtt서버에 접속 성공!")

                //접속성공하면 subscribe하도록 메소드 정의해 놓은 것을 호출
                //매개변수로 전달된 모든 topic들에 subscribeTopic 메소드 호출하며 설정
                topics?.map {
                    subscribeTopic(it)
                }
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("mymqtt","mqtt서버에 접속 실패!")

            }
        })
    }

    fun mysetCallback(callback:(topic:String,message:MqttMessage) -> Unit){ //아무것도 return하지 않음
        //mqttClient에 callback등록
        mqttClient.setCallback(object:MqttCallback{//콜백객체를 상속하고 있는 객체를 넣어줌
        override fun connectionLost(cause: Throwable?) { //연결이 끊어졌을 때
            Log.d("mymqtt","mqtt서버에 connectionLost")
        }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                //액티비티에 정의해 놓은 콜백메소드를 호출하면서 브로커로 부터 전달받은 메시지와 토픽을 전달
                callback(topic!!,message!!)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { //전송이 보내졌을 때
                Log.d("mymqtt","mqtt서버에  deliveryComplete")
            }

        })
    }

    /*subscribe 메소드*/
    private fun subscribeTopic(topic:String,qos:Int=0){
        mqttClient.subscribe(topic,qos,null,object:IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("mymqtt","mqtt서버에 subscribe 성공!")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("mymqtt","mqtt서버에 subscribe 실패!")
            }

        })
    }

    /*publish 메소드*/
    fun publish(topic:String, payload:String, qos:Int =1) {
        if(mqttClient.isConnected == false){
            Log.d("mymqtt","mqtt클라이어트의 접속 상태가 false")
            mqttClient.connect()
        }
        //payload가 우리가 보낼 메시지, 이 메시지를 담고 있을 객체를 만든다 (mqttMessage객체에 담아서 보내야함)
        val message = MqttMessage()
        //String으로 만들어진 메시지를 byte배열로 변환(인터넷 통신을 하기 때문)
        message.payload = payload.toByteArray()
        message.qos = qos
        mqttClient.publish(topic,message,null,object:IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("mymqtt","mqtt서버에 publish 성공!")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("mymqtt","mqtt서버에 publish 실패!")
            }

        })
    }
}