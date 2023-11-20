-- Als Postgres User:
create user weatherdb PASSWORD 'weatherdb';
create database weatherdb with owner weatherdb;


-- Als WeatherDB user:
create table weather
(
    id          serial,
    region      VARCHAR(200) not null,
    city        varchar(200) not null,
    temperature float        not null
);


INSERT INTO public.weather (id, region, city, temperature) VALUES (DEFAULT, 'Europe', 'Vienna', 28);
INSERT INTO public.weather (id, region, city, temperature) VALUES (DEFAULT, 'Europe', 'Berlin', 26);
INSERT INTO public.weather (id, region, city, temperature) VALUES (DEFAULT, 'Asia', 'Tokyo', 18);
INSERT INTO public.weather (id, region, city, temperature) VALUES (DEFAULT, 'Europe', 'Rome', 35)




