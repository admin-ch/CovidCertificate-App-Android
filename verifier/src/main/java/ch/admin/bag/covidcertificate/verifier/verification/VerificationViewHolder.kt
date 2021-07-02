package ch.admin.bag.covidcertificate.verifier.verification

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.verifier.databinding.ItemVerificationStatusBinding
import ch.admin.bag.covidcertificate.verifier.databinding.ItemVerificationStatusInfoBinding

class StatusViewHolder(private val binding: ItemVerificationStatusBinding) : RecyclerView.ViewHolder(binding.root) {

	fun bindStatusItem(statusItem: StatusItem) {
		binding.apply {
			verificationStepTitle.text = statusItem.statusString
			verificationStepProgressIndicator.progressIndicatorIcon.setImageResource(statusItem.statusIcon)
			verificationStepProgressIndicator.progressIndicatorIcon.isVisible = !statusItem.isLoading
			verificationStepProgressIndicator.progressIndicatorProgressBar.isVisible = statusItem.isLoading
			verificationStepFrame.backgroundTintList =
				ColorStateList.valueOf(ContextCompat.getColor(root.context, statusItem.bubbleColor))
		}
	}

}

class InfoViewHolder(
	private val binding: ItemVerificationStatusInfoBinding,
	private val onRetryClickListener: View.OnClickListener
) : RecyclerView.ViewHolder(binding.root) {

	fun bindStatusItem(infoItem: InfoItem) {
		binding.apply {
			infoText.text = infoItem.statusString
			infoIcon.imageTintList =
				ColorStateList.valueOf(ContextCompat.getColor(root.context, infoItem.infoIconColor))
			infoFrame.backgroundTintList =
				ColorStateList.valueOf(ContextCompat.getColor(root.context, infoItem.bubbleColor))

			if (infoItem.showRetry) {
				infoRetry.isVisible = true
				infoRetry.setOnClickListener { view ->
					onRetryClickListener.onClick(view)
					infoRetry.setOnClickListener(null)
				}
			} else {
				infoRetry.isVisible = false
			}
		}
	}
}