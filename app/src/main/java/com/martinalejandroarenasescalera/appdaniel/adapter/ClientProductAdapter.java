package com.martinalejandroarenasescalera.appdaniel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.martinalejandroarenasescalera.appdaniel.R;
import com.martinalejandroarenasescalera.appdaniel.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * ClientProductAdapter – RecyclerView para el catálogo del cliente.
 * Muestra tarjetas de producto con imagen, nombre, precio y botón "Agregar al carrito".
 */
public class ClientProductAdapter extends RecyclerView.Adapter<ClientProductAdapter.ViewHolder> {

    public interface OnAddToCartListener {
        void onAddToCart(Product product);
    }

    private final List<Product> products = new ArrayList<>();
    private final OnAddToCartListener listener;

    public ClientProductAdapter(OnAddToCartListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> newList) {
        products.clear();
        if (newList != null) products.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_client, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() { return products.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName, tvPrice, tvStockBadge;
        private final Button btnAddToCart;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStockBadge = itemView.findViewById(R.id.tvStockBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }

        void bind(Product product) {
            Context ctx = itemView.getContext();

            tvName.setText(product.getName());
            tvPrice.setText(String.format("$%.2f", product.getPrice()));

            if (product.hasStock()) {
                tvStockBadge.setText(R.string.in_stock);
                tvStockBadge.setTextColor(ctx.getColor(R.color.stock_ok));
                btnAddToCart.setEnabled(true);
                btnAddToCart.setText(R.string.add_to_cart);
                btnAddToCart.setOnClickListener(v -> listener.onAddToCart(product));
            } else {
                tvStockBadge.setText(R.string.out_of_stock);
                tvStockBadge.setTextColor(ctx.getColor(R.color.stock_empty));
                btnAddToCart.setEnabled(false);
                btnAddToCart.setText(R.string.out_of_stock);
            }

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(ctx)
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.ic_product_placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imgProduct);
            } else {
                imgProduct.setImageResource(R.drawable.ic_product_placeholder);
            }
        }
    }
}
