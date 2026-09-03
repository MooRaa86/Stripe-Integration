<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Payment Cancelled</title>
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
            background: #fef3c7;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
        }

        .icon svg {
            width: 32px;
            height: 32px;
            color: #d97706;
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
                      d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374
                         1.948 3.374h14.71c1.73 0 2.813-1.874
                         1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898
                         0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
        </div>
        <h1>Payment Cancelled</h1>
        <p>Your payment was not completed. No charges have been made to your account.</p>
        <p>If you would like to try again, please return to the checkout page.</p>
        <div class="footer">
            Stripe Payment Integration &mdash; Educational Project
        </div>
    </div>
</body>
</html>
