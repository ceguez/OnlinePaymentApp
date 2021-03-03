package com.springBootApps.OnlinePaymentApp.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.springBootApps.OnlinePaymentApp.domain.Client;
import com.springBootApps.OnlinePaymentApp.repositories.ClientRepository;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

@Controller
public class ClientBookingControler {
	
	private static final String SERVICE_MONTHLY_PLAN = "service-monthly-plan";
	@Autowired
	private ClientRepository clientRepo;
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String indexPage (ModelMap model) {
		
		model.put("client", new Client());
		return "index";
	}
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public String indexPagePost(@ModelAttribute Client client) {
		
		System.out.println(client);
		clientRepo.save(client);
		System.out.println(client);
		return "redirect:/deposit?email=" + client.getEmail();
	}
	
	@RequestMapping(value = "/deposit", method = RequestMethod.GET)
	public String depositPage(@RequestParam String email, ModelMap model) { // parameter name must match email otherwise it is more syntax so it does map
		
		Set<Client> clients = clientRepo.findByEmail(email);
		if(clients != null && clients.size() > 0){
			Client client = clients.iterator().next();
			model.put("client", client);
		} else {
			model.put("client", new Client());
		}
		return "deposit";
	}
	
	@RequestMapping(value = "/deposit", method = RequestMethod.POST)
	public String depositPagePost(@RequestParam(name = "stripeToken") String token, @RequestParam String stripeEmail) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		//secrete key API
		Stripe.apiKey = "sk_test_51IQGRnLRPZMtjEYBZqqciWI925LS1VZSpqooV3NsElHv1iWlsAX6VmuQLyBHl5ZfxVKmW3x9aNAaE6szNgQk4dDq00aXlFl8x1";
	
		//Create a Customer
		Map<String, Object> customerParams = new HashMap<String, Object>();
		customerParams.put("email", stripeEmail);
		customerParams.put("source", token);
		Customer customer = Customer.create(customerParams);
		
		Set<Client> clients = clientRepo.findByEmail(stripeEmail);
		
		Iterator<Client> itr = clients.iterator();
		
		Optional<Client> clientOpt = itr.hasNext() ? Optional.of(itr.next()) : Optional.empty();
		Client client = null;
		
		if (clientOpt.isPresent()) {
			client = clientOpt.get();
		} else {
			throw new IllegalArgumentException("There was no client with email address: " + stripeEmail + " found");
		}
		
		client.setStripeId(customer.getId());
		clientRepo.save(client);

		//Charge the customer instead of the card:
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		chargeParams.put("amount", 10000);
		chargeParams.put("currency" , "usd");
		chargeParams.put("customer", customer.getId());
		Charge.create(chargeParams);
		
		return "redirect:/thankyou?email=" + stripeEmail;
	}
	
	@RequestMapping (value = "/thankyou", method = RequestMethod.GET)
	public String thankyouPage (@RequestParam String email , ModelMap model) {
		
		model.put("email", email);
		return "thankyou";
	}
	
	@RequestMapping (value = "/thankyou", method = RequestMethod.POST)
	public String thankyouPagePost (@RequestParam String email, ModelMap model) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
		
		Set<Client> clients = clientRepo.findByEmail(email);
		
		Iterator<Client> itr = clients.iterator();
		
		if(itr.hasNext()) {
			
			Client client = itr.next();
			
			if (!ObjectUtils.isEmpty(client.getStripeId())){
				
				//To charge customer again if answered yes on promotion. To charge again, retrieve the customer ID.
				Map<String, Object> chargeParams = new HashMap<String, Object>();
				chargeParams.put("amount", 26000);
				chargeParams.put("currency" , "usd");
				chargeParams.put("customer", client.getStripeId());
				Charge.create(chargeParams);
			}
		}
		return "redirect:thankyou2";
	}
	
	 @RequestMapping(value = "/subscribe", method = RequestMethod.POST)
	  public String subscribePost(@RequestParam(name = "email") String email)
	      throws AuthenticationException, InvalidRequestException,
	      APIConnectionException, CardException, APIException
	  {
	    createStripePlanIfNeeded();

	    // Set your secret key: remember to change this to your live secret key in
	    // production
	    // See your keys here: https://dashboard.stripe.com/account/apikeys
	    Stripe.apiKey = "sk_test_51IQGRnLRPZMtjEYBZqqciWI925LS1VZSpqooV3NsElHv1iWlsAX6VmuQLyBHl5ZfxVKmW3x9aNAaE6szNgQk4dDq00aXlFl8x1";

	    Set<Client> clients = clientRepo.findByEmail(email);
	    
	    Optional<Client> clientOpt = clients.stream().filter(c -> !ObjectUtils.isEmpty(c.getStripeId()))
	                                        .findFirst();
	    
	    if (clientOpt.isPresent())
	    {
	      Client client = clientOpt.get();
	      
	      Map<String, Object> params = new HashMap<String, Object>();
	      params.put("customer", client.getStripeId());
	      params.put("plan", SERVICE_MONTHLY_PLAN);
	      
	      Subscription.create(params);
	    }

	    return "redirect:subscribeSuccess";
	  }
	 
		@RequestMapping (value = "/thankyou2", method = RequestMethod.GET)
		public String thankyouPage2 (ModelMap model) {
			
			return "thankyou2";
		}
		
		 
		 @RequestMapping(value = "/subscribeSuccess", method = RequestMethod.GET)
		  public String subscribeSuccessGet ()
		  {
		    return "subscribeSuccess";
		  }

	    private void createStripePlanIfNeeded()
	    	      throws AuthenticationException, APIConnectionException, CardException,
	    	      APIException, InvalidRequestException
	    	  {
	    	    // Set your secret key: remember to change this to your live secret key in
	    	    // production
	    	    // See your keys here: https://dashboard.stripe.com/account/apikeys
	    	    Stripe.apiKey = "sk_test_51IQGRnLRPZMtjEYBZqqciWI925LS1VZSpqooV3NsElHv1iWlsAX6VmuQLyBHl5ZfxVKmW3x9aNAaE6szNgQk4dDq00aXlFl8x1";

	    	    try
	    	    {
	    	      Plan.retrieve(SERVICE_MONTHLY_PLAN);
	    	    } catch (InvalidRequestException e){
	    	    	
	    	      // this will throw an invalid requestexception if the plan doesn't exist
	    	      Map<String, Object> params = new HashMap<String, Object>(); 
	    	      params.put("nickname", "Service Monthly Plan");
	    	      params.put("product", "prod_J2rJicLsgdvCVM");
	    	      params.put("id", SERVICE_MONTHLY_PLAN);
	    	      params.put("interval", "month");
	    	      params.put("currency", "usd");
	    	      params.put("amount", 20000);

	    	      Plan.create(params);
	    	    }
	    	  }
	
}


