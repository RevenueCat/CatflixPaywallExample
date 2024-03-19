//
//  ContentView.swift
//  observermode
//
//  Created by Greenie on 07/03/2024.
//

import SwiftUI
import StoreKit

enum SubscriptionPlan: String, CaseIterable {
    case annual = "Annual Plan"
    case monthly = "Monthly Plan"
    
    var productIdentifier: String {
        switch self {
        case .annual: return "com.revenuecat.nocode.annual"
        case .monthly: return "com.revenuecat.nocode.monthly"
        }
    }
}

class ProductStore: NSObject, ObservableObject, SKProductsRequestDelegate, SKPaymentTransactionObserver {
    
    @Published var products: [SKProduct] = []
    
    override init() {
        super.init()
        SKPaymentQueue.default().add(self)
    }
    
    func requestProducts() {
        let productIdentifiers = Set(SubscriptionPlan.allCases.map { $0.productIdentifier })
        let request = SKProductsRequest(productIdentifiers: productIdentifiers)
        request.delegate = self
        request.start()
    }
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        if !response.products.isEmpty {
            DispatchQueue.main.async {
                self.products = response.products
            }
        }
    }
    
    func buyProduct(_ productIdentifier: String) {
        guard let product = products.first(where: {$0.productIdentifier == productIdentifier }) else {
            print("Product not found: \(productIdentifier)")
            return
        }
        let payment = SKPayment(product: product)
        SKPaymentQueue.default().add(payment)
    }
    
    func restorePurchases() {
        SKPaymentQueue.default().restoreCompletedTransactions()
    }
    
    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased, .restored:
                SKPaymentQueue.default().finishTransaction(transaction)
            case .failed:
                SKPaymentQueue.default().finishTransaction(transaction)
            default:
                break
            }
        }
    }
    
    func paymentQueueRestoreCompletedTransactionsFinished(_ queue: SKPaymentQueue) {
        // Called when the restore purchases operation is finished
    }
}

// A custom radio button view
struct RadioButton: View {
    let id: SubscriptionPlan
    let label: String
    let productIdentifier: String
    let isSelected: Bool
    let action: (SubscriptionPlan) -> Void
    
    var body: some View {
        Button(action: { self.action(self.id) }) {
            HStack {
                Image(systemName: self.isSelected ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(self.isSelected ? .red : .gray)
                    .accessibility(label: Text(self.isSelected ? "Selected" : "Not Selected"))
                Text(label)
                    .foregroundColor(.black)
                Text(productIdentifier)
                    .foregroundColor(.black)
                    .fontWeight(.thin)
            }
        }
        .foregroundColor(.black)
    }
}

// A custom group to manage radio buttons
struct RadioButtonGroup: View {
    let plans: [SubscriptionPlan]
    @Binding var selectedPlan: SubscriptionPlan
    
    var body: some View {
        ForEach(plans, id: \.self) { plan in
            RadioButton(
                id: plan,
                label: plan.rawValue,
                productIdentifier: plan.productIdentifier,
                isSelected: plan == self.selectedPlan,
                action: {
                    self.selectedPlan = $0
                }
            )
            .padding(.vertical, 4)
        }
    }
}



struct ContentView: View {
    @StateObject var store = ProductStore()
    @State private var selectedPlan: SubscriptionPlan = .annual
    
    
    var body: some View {
        VStack {
            Text("CATFLIX")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.red)
            
            RadioButtonGroup(plans: SubscriptionPlan.allCases, selectedPlan: $selectedPlan)
            
            Button(action: {store.buyProduct(selectedPlan.productIdentifier)}) {
                Text("Purchase")
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red)
                    .cornerRadius(10)
            }
            .padding()
            Button(action: {store.restorePurchases()}) {
                Text("Restore")
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(5)
            }
            .padding()
        }
        .onAppear {
            store.requestProducts()
        }.padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
    }
}

#Preview {
    ContentView()
}


