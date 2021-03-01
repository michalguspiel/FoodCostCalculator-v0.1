package com.example.foodcostcalc.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.example.foodcostcalc.R
import com.example.foodcostcalc.fragments.dialogs.EditDish
import com.example.foodcostcalc.model.DishWithProductsIncluded
import com.example.foodcostcalc.model.GrandDish
import com.example.foodcostcalc.viewmodel.AddViewModel
import com.example.foodcostcalc.viewmodel.HalfProductsViewModel
import java.util.ArrayList


class DishAdapter(
    val tag: String?,
    private val list: ArrayList<GrandDish>,
    private val fragmentManager: FragmentManager,
    val viewModel: AddViewModel,
    val halfProductsViewModel: HalfProductsViewModel,
    val viewLifecycleOwner: LifecycleOwner,
    val activity: Activity
) : RecyclerView.Adapter<DishAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eachLinearLayout: LinearLayout = view.findViewById(R.id.linear_layout_dish_card)
        val dishNameTextView: TextView = view.findViewById(R.id.dish_name_in_adapter)
        val dishMarginTextView: TextView = view.findViewById(R.id.dish_margin_in_adapter)
        val dishTaxTextView: TextView = view.findViewById(R.id.dish_tax_in_adapter)
        val editButton: ImageButton = view.findViewById(R.id.edit_button_in_dish_adapter)
        val listView: ListView = view.findViewById(R.id.list_view)
        val totalPriceOfDish: TextView = view.findViewById(R.id.total_price_dish_card_view)
        val finalPriceWithMarginAndTax: TextView =
            view.findViewById(R.id.total_price_with_margin_dish_card_view)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val adapterLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.dish_card_view, parent, false)
        return RecyclerViewHolder(adapterLayout)

    }

    override fun getItemCount(): Int {
        return list.size
    }


    @SuppressLint("WrongConstant", "ShowToast", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        /**Computes height of listView based on each row height, includes dividers.
         * I'm using this approach so listView size is set and doesn't need to be scrollable. */
        fun getListSize(): Int {
            var result = 0
            for (eachProduct in list[position].productsIncluded.indices + // first products included
                    list[position].halfProducts.indices) { // plus halfproducts
                val listItem = holder.listView.adapter.getView(eachProduct, null, holder.listView)
                listItem.measure(0, View.MeasureSpec.UNSPECIFIED)
                result += listItem.measuredHeight
            }
            return result + (holder.listView.dividerHeight * (holder.listView.adapter.count - 1))
        }
        fun setPrice() { // TODO why doesnt it work???
            var priceOfHalfProducts = 0.0
            priceOfHalfProducts += list[position].totalPrice
            list[position].halfProducts.forEach{
                var price = 0.0
                halfProductsViewModel
                    .getCertainHalfProductWithProductsIncluded(it.halfProductOwnerId)
                    .observe(viewLifecycleOwner, Observer { halfProductWithProductsIncluded ->
                        price = (halfProductWithProductsIncluded.pricePerUnit())
                        Log.i("test",halfProductWithProductsIncluded.pricePerUnit().toString())
                    })
                Log.i("test2",price.toString())
                priceOfHalfProducts += price
            }
            holder.totalPriceOfDish.text = priceOfHalfProducts.toString()
        }


        holder.dishNameTextView.text = list[position].dish.name
        holder.dishMarginTextView.text = "Margin: ${list[position].dish.marginPercent}%"
        holder.dishTaxTextView.text = "Tax: ${list[position].dish.dishTax}%"
        setPrice()
        holder.finalPriceWithMarginAndTax.text =
            list[position].formattedPriceWithMarginAndTax  // TODO  sum with half products total price

        holder.editButton.setOnClickListener {
            EditDish().show(fragmentManager, EditDish.TAG)
            EditDish.dishPassedFromAdapter = list[position]
        }

        holder.eachLinearLayout.setOnClickListener {
            if (holder.listView.adapter == null) {
                holder.listView.adapter = DishListViewAdapter(
                    activity,
                    list[position],
                    halfProductsViewModel,
                    viewLifecycleOwner
                )
                holder.listView.layoutParams =
                    LinearLayout.LayoutParams(holder.listView.layoutParams.width, getListSize())
            } else {
                holder.listView.adapter = null
                holder.listView.layoutParams =
                    LinearLayout.LayoutParams(holder.listView.layoutParams.width, 0)
            }
        }

    }
}