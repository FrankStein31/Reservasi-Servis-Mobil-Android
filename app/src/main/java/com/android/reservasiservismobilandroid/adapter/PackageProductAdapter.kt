package com.android.reservasiservismobilandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.databinding.ItemPackageProductBinding
import java.text.NumberFormat
import java.util.Locale

class PackageProductAdapter(
    private val products: List<Map<String, Any>>
) : RecyclerView.Adapter<PackageProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemPackageProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], position + 1)
    }

    override fun getItemCount() = products.size

    inner class ProductViewHolder(private val binding: ItemPackageProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Map<String, Any>, position: Int) {
            binding.tvProductNumber.text = position.toString()
            binding.tvProductName.text = product["name"].toString()
            
            val price = when (val priceValue = product["price"]) {
                is Int -> priceValue.toDouble()
                is Double -> priceValue
                is String -> priceValue.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            binding.tvProductPrice.text = currencyFormat.format(price)
        }
    }
} 