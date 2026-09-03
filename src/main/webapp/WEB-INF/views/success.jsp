<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payment Successful</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background: #f6f9fc;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #1a1a2e;
        }

        .card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
            padding: 48px 40px;
            max-width: 480px;
            width: 90%;
            text-align: center;
        }

        .icon {
            width: 64px;
            height: 64px;
            border-radius: 50%;
            background: #d1fae5;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
        }

        .icon svg {
            width: 32px;
            height: 32px;
            color: #059669;
        }

        h1 {
            font-size: 1.5rem;
            font-weight: 600;
            margin-bottom: 12px;
            color: #111827;
        }

        p {
            font-size: 0.95rem;
            color: #6b7280;
            line-height: 1.6;
            margin-bottom: 8px;
        }

        .footer {
            margin-top: 32px;
            padding-top: 20px;
            border-top: 1px solid #f3f4f6;
            font-size: 0.8rem;
            color: #9ca3af;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none"
                 viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M4.5 12.75l6 6 9-13.5" />
            </svg>
        </div>
        <h1>Payment Successful</h1>
        <p>Thank you for your purchase. Your payment has been processed successfully and your order is being fulfilled.</p>
        <p>A confirmation has been recorded on our end.</p>
        <div class="footer">
            Stripe Payment Integration &mdash; Educational Project
        </div>
    </div>
</body>
</html>
