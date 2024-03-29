const grpc=require('grpc');
const PROTO_PATH="../src/main/proto/db.proto";
const db=grpc.load(PROTO_PATH);
const config=require('../config.json');
const bluebird=require('bluebird');
const assert=require('assert');
bluebird.promisifyAll(db.blockdb.BlockDatabase.prototype);
const stub=new db.blockdb.BlockDatabase("127.0.0.1"+":"+config["1"]["port"], grpc.credentials.createInsecure(),
{
  'grpc.min_reconnect_backoff_ms': 1000,
  'grpc.max_reconnect_backoff_ms': 1000,
}
);
//Function to start a server that does not crash.
async function startStable(){


}
//Function to start a server that often crashes.
async function startUnstable(){

}
const client={
	"LogLength": async ()=>{
		try{
			return (await stub.logLengthAsync({})).Value;
		}catch(err){
            //console.log(err)
			console.log("Error caught in LogLength! Retrying...");
			return await client.LogLength();
		}
	},
	"GET": async(user)=>{
		try{
			return (await stub.getAsync({"UserID": user})).Value;
		}catch(err){
			console.log("Error caught in GET! Retrying...");
			return await client.GET(user);
			
		}
	},
	"PUT": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.putAsync({"UserID": user, "Value": val})).Success;
		}catch(err){
			console.log("Error caught in PUT!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.PUT(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
	},
	"DEPOSIT": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.depositAsync({"UserID": user, "Value": val})).Success;
		}catch(err){
			console.log("Error caught in DEPOSIT!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.DEPOSIT(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
	},
	"WITHDRAW": async(user, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.withdrawAsync({"UserID": user, "Value": val})).Success;
		}catch(err){
			console.log("Error caught in WITHDRAW!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.WITHDRAW(user, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
		
	},
	"TRANSFER": async(ufrom, uto, val)=>{
		const last=await client.LogLength();
		try{
			return (await stub.transferAsync({"FromID": ufrom, "ToID": uto, "Value": val})).Success;
		}catch(err){
			console.log("Error caught in TRANSFER!");
			const curr=await client.LogLength();
			if(last==curr){
				console.log("Retrying...");
				return await client.TRANSFER(ufrom, uto, val);
			}else{
				console.log("Log added. Not retrying.");
				return true;
			}
		}
		
	}
	
}

async function test(){
	console.log("Step 1: Initialize account")
	for(let I=0;I<=9;I++){
		assert(await client.PUT(`TEST---${I}`,10));
	}
	console.log("Check value: expecting value=10")
	assert(10==await client.GET("TEST---5"));

	console.log("Check LogLength: expecting value=10")
	assert(10==await client.LogLength());

	console.log("Step 2: Try deposit")
	for(let I=0;I<=9;I++){
		assert(await client.DEPOSIT(`TEST---${I}`,5))
	}
	console.log("Check value: expecting value=15")
	assert(15==await client.GET("TEST---5"));

	console.log("Step 3: Try transfer")
	for(let I=0;I<=9;I++){
		assert(await client.TRANSFER(`TEST---${I}`,"TEST--TX" ,10));
	}
	console.log("Check value: expecting value=100")
	assert(100==await client.GET("TEST--TX"));

	console.log("Step 4: Try transfer again")
	for(let I=0;I<=9;I++){
		assert(await client.TRANSFER("TEST--TX" ,`TEST---${I}` ,5));
	}
	console.log("Check value: expecting value=50")
	assert(50==await client.GET("TEST--TX"));

	console.log("Step 5: Try withdraw")
	for(let I=0;I<=9;I++){
		assert(await client.WITHDRAW(`TEST---${I}` ,5));
	}
	console.log("Check value: expecting value=5");
        assert(5==await client.GET("TEST---2"));

	console.log("Step 5: Try overdraft")
	for(let I=0;I<=9;I++){
		assert(!await client.WITHDRAW(`TEST---${I}` ,10));
	}
	console.log("Check value: expecting value=5")
	assert(5==await client.GET("TEST---2"));

	console.log("Check LogLength: expecting value<=50")
	assert(50>=await client.LogLength())

	//console.log "Try Killing the server and restart..."
	//console.log "Sleep for a while, waiting for hashmap reconstruction..."
	//sleep 10;

	console.log("Check value again: expecting value=5")
	assert(5==await client.GET("TEST---2"));


	console.log("Test completed. Please verify JSON block output with example_1.json .")
	
}
test().then(console.log).catch(console.log);
