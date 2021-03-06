package com.erdees.foodcostcalc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.erdees.foodcostcalc.data.AppRoomDataBase
import com.erdees.foodcostcalc.data.basic.BasicDataBase
import com.erdees.foodcostcalc.data.basic.BasicRepository
import com.erdees.foodcostcalc.data.grandDish.GrandDishRepository

class DishesViewModel(application: Application): AndroidViewModel(application) {

    val grandDishRepository: GrandDishRepository
    val basicRepository : BasicRepository

    init {
        val grandDishDao = AppRoomDataBase.getDatabase(application).grandDishDao()
        val basicDao = BasicDataBase.getInstance().basicDao

        grandDishRepository = GrandDishRepository(grandDishDao)
        basicRepository = BasicRepository(basicDao)
    }

    fun getGrandDishes() = grandDishRepository.getGrandDishes()

    fun getWhatToSearchFor() = basicRepository.getWhatToSearchFor()
}