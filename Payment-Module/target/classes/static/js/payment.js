// ==============================================================
// Taxi & Cab Booking System - Payment Frontend Logic
// ==============================================================

document.addEventListener('DOMContentLoaded', function () {
    const methodOptions = document.querySelectorAll('.method-option');
    const methodInput = document.getElementById('paymentMethod');
    const cashSection = document.getElementById('cash-fields');
    const cardSection = document.getElementById('card-fields');
    const onlineSection = document.getElementById('online-fields');

    const amountTenderedInput = document.getElementById('amountTendered');
    const changePreview = document.getElementById('changePreview');
    const finalAmountHidden = document.getElementById('finalAmount');
    const finalAmountDisplay = document.getElementById('finalAmountDisplay');

    // Method selection switcher
    methodOptions.forEach(opt => {
        opt.addEventListener('click', function () {
            methodOptions.forEach(o => o.classList.remove('selected'));
            this.classList.add('selected');
            const method = this.getAttribute('data-method');
            if (methodInput) {
                methodInput.value = method;
            }
            updateMethodFields(method);
        });
    });

    function updateMethodFields(method) {
        if (cashSection) cashSection.style.display = (method === 'CASH') ? 'block' : 'none';
        if (cardSection) cardSection.style.display = (method === 'CARD') ? 'block' : 'none';
        if (onlineSection) onlineSection.style.display = (method === 'ONLINE') ? 'block' : 'none';
    }

    // Cash Change live calculation
    if (amountTenderedInput && changePreview && finalAmountHidden) {
        amountTenderedInput.addEventListener('input', function () {
            const tendered = parseFloat(this.value) || 0;
            const payable = parseFloat(finalAmountHidden.value) || 0;
            if (tendered >= payable) {
                const change = tendered - payable;
                changePreview.textContent = 'Change to return: $' + change.toFixed(2);
                changePreview.style.color = '#059669';
            } else {
                const short = payable - tendered;
                changePreview.textContent = 'Short by: $' + short.toFixed(2);
                changePreview.style.color = '#dc2626';
            }
        });
    }

    // Quick promo code chip click
    const promoChips = document.querySelectorAll('.promo-chip');
    const promoInput = document.getElementById('promoCodeInput');
    promoChips.forEach(chip => {
        chip.addEventListener('click', function () {
            if (promoInput) {
                promoInput.value = this.getAttribute('data-code');
                applyPromoAsync();
            }
        });
    });

    // Real-time AJAX Promo Code validation
    const applyPromoBtn = document.getElementById('applyPromoBtn');
    if (applyPromoBtn) {
        applyPromoBtn.addEventListener('click', function (e) {
            e.preventDefault();
            applyPromoAsync();
        });
    }

    function applyPromoAsync() {
        const code = promoInput ? promoInput.value.trim() : '';
        const baseFareEl = document.getElementById('baseFare');
        const baseFare = baseFareEl ? parseFloat(baseFareEl.value) : 0;
        const feedbackEl = document.getElementById('promoFeedback');

        if (!code) {
            if (feedbackEl) {
                feedbackEl.textContent = 'Please enter a promo code.';
                feedbackEl.className = 'promo-feedback text-danger';
            }
            return;
        }

        fetch(`/api/promotions/validate?code=${encodeURIComponent(code)}&fare=${baseFare}`)
            .then(res => res.json())
            .then(data => {
                if (data.valid) {
                    if (feedbackEl) {
                        feedbackEl.textContent = data.message;
                        feedbackEl.className = 'promo-feedback text-success';
                    }
                    if (finalAmountDisplay) {
                        finalAmountDisplay.textContent = '$' + data.finalAmount.toFixed(2);
                    }
                    if (finalAmountHidden) {
                        finalAmountHidden.value = data.finalAmount.toFixed(2);
                    }
                    const discountDisplay = document.getElementById('discountDisplay');
                    if (discountDisplay) {
                        discountDisplay.textContent = '-$' + data.discountAmount.toFixed(2);
                    }
                    // Trigger change recalculation if cash tendered is filled
                    if (amountTenderedInput) {
                        amountTenderedInput.dispatchEvent(new Event('input'));
                    }
                } else {
                    if (feedbackEl) {
                        feedbackEl.textContent = data.message;
                        feedbackEl.className = 'promo-feedback text-danger';
                    }
                }
            })
            .catch(err => {
                console.error('Error validating promo:', err);
            });
    }
});
