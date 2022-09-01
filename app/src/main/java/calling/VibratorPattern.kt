package com.example.vibrateexam

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.os.VibrationEffect
import android.os.VibrationEffect.*
import android.os.Vibrator
import androidx.core.content.ContextCompat.getSystemService

class VibratorPattern(val context: Context) {
     private var vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

     fun simplePattern(){
          val effect = VibrationEffect.createOneShot(500, 100)
          vibrator.vibrate(effect)
     }

     fun startAndFinishPattern(){ //출발,도착
          val timing = longArrayOf(0,200, 0,200, 0,200, 0,200, 0,200, 0,200, 0,200)
          val amplitudes = intArrayOf(0,100, 0,95, 0,90, 0,85, 0,80, 0,75, 0,70) //세기
          val effect = VibrationEffect.createWaveform(timing,amplitudes,-1)
          vibrator.vibrate(effect)
     }

     fun pattern(){ //위험요소
          val timing = longArrayOf(0,250, 50,750)
          val amplitudes = intArrayOf(0,100, 0,255) //세기
          val effect = VibrationEffect.createWaveform(timing,amplitudes,-1)
          vibrator.vibrate(effect)
     }

     fun outPattern(){ //경로이탈
          val timing = longArrayOf(0,250, 250,250, 50,250, 50,250, 550,250, 250,250, 50,250, 50,250)
          val amplitudes = intArrayOf(0,255, 0,50, 0,50, 0,100, 0,255, 0,50, 0,50, 0,100) //세기
          val effect = VibrationEffect.createWaveform(timing,amplitudes,-1)
          vibrator.vibrate(effect)
     }

     //repeat 반복횟수 : -1 반복x 0이상은 그 인덱스부터 무한반복 만약 4면 012345454545 일케됨
     fun rightPattern(){ //징징 징징 징징
          val timing = longArrayOf(0,500, 0,500, 750,500, 0,500)
          val amplitudes = intArrayOf(0,100, 0,100, 0,100, 0,100) //세기
          val effect = VibrationEffect.createWaveform(timing,amplitudes,4)
          vibrator.vibrate(effect)
     }

     fun leftPattern(){//징 징 징
          val timing = longArrayOf(0,500, 750,500)
          val amplitudes = intArrayOf(0,100, 0,100) //세기
          val effect = VibrationEffect.createWaveform(timing,amplitudes,2)
          vibrator.vibrate(effect)
     }

     fun stopVibrator(){ //right&left종료
          vibrator.cancel()
     }
}